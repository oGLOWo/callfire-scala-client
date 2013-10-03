package com.oglowo.callfire.json

import spray.json._
import com.oglowo.callfire.entity._
import com.oglowo.callfire.entity.ApiError
import com.oglowo.callfire.entity.PhoneNumber
import com.github.nscala_time.time.Imports._
import scala.xml.XML
import scala.Seq
import spray.http.Uri
import com.oglowo.callfire.Imports._
import org.joda.time.format.ISODateTimeFormat
import org.joda.money.{CurrencyUnit, Money}
import com.github.nscala_time.time.Imports._

object ApiEntityFormats extends DefaultJsonProtocol {
  val CallFireDateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-ddZ")
  val CallFireIsoDateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis

  implicit val ApiErrorFormat = new RootJsonFormat[ApiError] {
    def write(obj: ApiError): JsValue = ???

    def read(json: JsValue): ApiError = json match {
      case jsonResponse: JsObject => {
        jsonResponse.getFields("ResourceException").headOption match {
          case Some(exceptionJson) => {
            exceptionJson.asJsObject.getFields("HttpStatus", "Message") match {
              case Seq(JsNumber(status), JsString(message)) => ApiError(message, status.intValue)
              case _ => throw new DeserializationException("JSON did not contain required fields HttpStatus and Message")
            }
          }
          case _ => throw new DeserializationException("JSON did not contain ResourceException field")
        }
      }

      case default => throw new DeserializationException(s"Expected JsObject, but got ${default.getClass}")
    }
  }

  implicit val RegionFormat = new RootJsonFormat[Region] {
    def write(obj: Region): JsValue = ???

    def read(json: JsValue): Region = json match {
      case regionJson: JsObject => {
        val fields = regionJson.fields
        val prefix = fields.get("Prefix") match {
          case Some(JsNumber(value)) => Option(value.toLong)
          case _ => None
        }
        val city = fields.get("City") match {
          case Some(JsString(value)) => Option(value)
          case _ => None
        }
        val state = fields.get("State") match {
          case Some(JsString(value)) => Option(value)
          case _ => None
        }
        val postalCode = fields.get("Zipcode") match {
          case Some(JsNumber(value)) => Option(value.toLong)
          case _ => None
        }
        val country = fields.get("Country") match {
          case Some(JsString(value)) => Option(value)
          case _ => None
        }
        val localAccessTransportArea = fields.get("Lata") match {
          case Some(JsNumber(value)) => Option(value.toLong)
          case _ => None
        }
        val rateCenter = fields.get("RateCenter") match {
          case Some(JsString(value)) => Option(value)
          case _ => None
        }
        val latitude = fields.get("Latitude") match {
          case Some(JsNumber(value)) => Option(value.toFloat)
          case _ => None
        }
        val longitude = fields.get("Longitude") match {
          case Some(JsNumber(value)) => Option(value.toFloat)
          case _ => None
        }
        val timeZone = fields.get("TimeZone") match {
          case Some(JsString(value)) => Option(value)
          case _ => None
        }
        Region(prefix, city, state, postalCode, country, localAccessTransportArea, rateCenter, latitude, longitude, timeZone)
      }
      case _ => deserializationError("Failed to deserialize Region because it was not a JsonObject")
    }
  }

  implicit val LeaseFormat = new RootJsonFormat[Lease] {
    def write(obj: Lease): JsValue = JsObject(Map(
      "autoRenew" -> obj.autoRenew.toJson,
      "begin" -> optionFormat[DateTime].write(obj.begin),
      "end" -> optionFormat[DateTime].write(obj.end)
    ))

    def read(json: JsValue): Lease = json match {
      case leaseJson: JsObject => {
        leaseJson.getFields("AutoRenew") match {
          case Seq(JsBoolean(autoRenew)) => {
            val fields = leaseJson.fields
            val begin = fields.get("LeaseBegin") match {
              case Some(JsString(value)) => Option(CallFireDateTimeFormatter.parseDateTime(value))
              case _ => None
            }
            val end = fields.get("LeaseEnd") match {
              case Some(JsString(value)) => Option(CallFireDateTimeFormatter.parseDateTime(value))
              case _ => None
            }
            Lease(begin, end, autoRenew)
          }
          case _ => deserializationError("Failure deserializing LeaseInfo because required AutoRenew was not present")
        }
      }
      case _ => deserializationError("Failure deserializing LeaseInfo because it was not a JsObject")
    }
  }

  implicit val PhoneNumberConfigurationFormat = new RootJsonFormat[PhoneNumberConfiguration] {
    def write(obj: PhoneNumberConfiguration): JsValue = ???

    def read(json: JsValue): PhoneNumberConfiguration = json match {
      case phoneNumberConfigurationJson: JsObject => {
        val fields = phoneNumberConfigurationJson.fields
        val callFeature = fields.get("CallFeature") match {
          case Some(JsString(value)) => Option(PhoneNumberFeature.withName(value))
          case _ => None
        }
        val textFeature = fields.get("TextFeature") match {
          case Some(JsString(value)) => Option(PhoneNumberFeature.withName(value))
          case _ => None
        }
        val inboundCallConfigurationType = fields.get("InboundCallConfigurationType") match {
          case Some(JsString(value)) => Option(InboundCallConfigurationType.withName(value))
          case _ => None
        }

        fields.get("InboundCallConfiguration") match {
          case Some(JsObject(iccFields)) => {
            inboundCallConfigurationType match {
              case Some(IvrConfigurationType) => {
                iccFields.get("IvrInboundConfig") match {
                  case Some(JsObject(ivrConfigFields)) => {
                    val dialplanXml = ivrConfigFields.get("DialplanXml") match {
                      case Some(JsString(value)) => Option(XML.loadString(value))
                      case _ => None
                    }
                    val id = ivrConfigFields.get("@id") match {
                      case Some(JsString(value)) => Option(value.toLong)
                      case _ => None
                    }
                    PhoneNumberConfiguration(callFeature, textFeature, Some(InboundIvrConfiguration(dialplanXml, id)))
                  }
                  case _ => deserializationError("Failure deserializing NumberConfiguration because its InboundCallConfiguration object did not contain the IvrInboundConfig object")
                }
              }
              case Some(CallTrackingConfigurationType) => {
                iccFields.get("CallTrackingConfig") match {
                  case Some(JsObject(callTrackingConfigFields)) => {
                    val id: Option[Long] = callTrackingConfigFields.get("@id") match {
                      case Some(JsString(value)) => Option(value.toLong)
                      case _ => None
                    }
                    val transferNumbers: Set[String] = callTrackingConfigFields.get("TransferNumber") match {
                      case Some(JsString(value)) => value.split(" ").toSet
                      case _ => Set.empty
                    }
                    val screen: Boolean = callTrackingConfigFields.get("Screen") match {
                      case Some(JsBoolean(value)) => value
                      case _ => false
                    }
                    val record: Boolean = callTrackingConfigFields.get("Record") match {
                      case Some(JsBoolean(value)) => value
                      case _ => false
                    }
                    val introductionSoundId: Option[Long] = callTrackingConfigFields.get("IntroSoundId") match {
                      case Some(JsNumber(value)) => Option(value.toLong)
                      case _ => None
                    }
                    val whisperSoundId: Option[Long] = callTrackingConfigFields.get("WhisperSoundId") match {
                      case Some(JsNumber(value)) => Option(value.toLong)
                      case _ => None
                    }
                    PhoneNumberConfiguration(callFeature, textFeature, Some(CallTrackingConfiguration(transferNumbers, screen, record, introductionSoundId, whisperSoundId, id)))
                  }
                  case _ => deserializationError("Failure deserializing NumberConfiguration because its InboundCallConfiguration object did not contain the CallTrackingConfig object")
                }
              }
              case _ => PhoneNumberConfiguration(callFeature, textFeature, None)
            }
          }
          case _ => deserializationError("Failure deserializing NumberConfiguration because it did not have an InboundCallConfiguration object")
        }
      }
      case _ => deserializationError("Failed to deserialize PhoneNumberConfiguration because it was not a JsonObject")
    }
  }

  implicit val phoneNumberStatusFormat = new RootJsonFormat[PhoneNumberStatus] {
    def write(obj: PhoneNumberStatus): JsValue = obj.name.toJson

    def read(json: JsValue): PhoneNumberStatus = json match {
      case JsString(value) => PhoneNumberStatus.withName(value)
      case default => deserializationError(s"Expecting PhoneNumberStatus to be json string, but got ${default.getClass}")
    }
  }

  implicit val PhoneNumberFormat = new RootJsonFormat[PhoneNumber] {
    def write(obj: PhoneNumber): JsValue = ???

    def parsePhoneNumber(resourceJson: JsObject): PhoneNumber = {
      val entityJson = resourceJson.getFields("Number").head.asJsObject
      val fields = entityJson.getFields("Number", "NationalFormat", "TollFree")
      fields match {
        case Seq(JsNumber(number), JsString(nationalFormat), JsBoolean(tollFree)) => {
          val region: Option[Region] = entityJson.fields.get("Region") match {
            case Some(regionJson) => Option(regionJson.convertTo[Region])
            case _ => None
          }
          val status: Option[PhoneNumberStatus] = entityJson.fields.get("Status") match {
            case Some(JsString(value)) => Option(PhoneNumberStatus.withName(value))
            case _ => None
          }
          val lease: Option[Lease] = entityJson.fields.get("LeaseInfo") match {
            case Some(leaseJson) => Option(leaseJson.convertTo[Lease])
            case _ => None
          }
          val numberConfiguration: Option[PhoneNumberConfiguration] = entityJson.fields.get("NumberConfiguration") match {
            case Some(numberConfigurationJson) => Option(numberConfigurationJson.convertTo[PhoneNumberConfiguration])
            case _ => None
          }
          PhoneNumber(number.toLong, nationalFormat, tollFree, region, status, lease, numberConfiguration)
        }
        case _ => deserializationError("Failure deserializing PhoneNumber because required fields Number, NationalFormat, and TollFree were not all present")
      }
    }

    def read(json: JsValue): PhoneNumber = json match {
      case jsonResponse: JsObject => {
        val resourceJson = jsonResponse.getFields("Resource").head.asJsObject
        parsePhoneNumber(resourceJson)
      }
      case _ => deserializationError("Expecting JSON object")
    }
  }

  implicit val PhoneNumberSeqFormat = new RootJsonFormat[Seq[PhoneNumber]] {
    def write(obj: Seq[PhoneNumber]): JsValue = ???

    def parsePhoneNumbers(resourceJson: JsObject): Seq[PhoneNumber] = {
      resourceJson.getFields("Number").head match {
        case JsArray(elements) => {
          elements.map(_.asJsObject).map(entityJson => {
            val fields = entityJson.getFields("Number", "NationalFormat", "TollFree")
            fields match {
              case Seq(JsNumber(number), JsString(nationalFormat), JsBoolean(tollFree)) => {
                val region: Option[Region] = entityJson.fields.get("Region") match {
                  case Some(regionJson) => Option(regionJson.convertTo[Region])
                  case _ => None
                }
                val status: Option[PhoneNumberStatus] = entityJson.fields.get("Status") match {
                  case Some(JsString(value)) => Option(PhoneNumberStatus.withName(value))
                  case _ => None
                }
                val lease: Option[Lease] = entityJson.fields.get("LeaseInfo") match {
                  case Some(leaseJson) => Option(leaseJson.convertTo[Lease])
                  case _ => None
                }
                val numberConfiguration: Option[PhoneNumberConfiguration] = entityJson.fields.get("NumberConfiguration") match {
                  case Some(numberConfigurationJson) => Option(numberConfigurationJson.convertTo[PhoneNumberConfiguration])
                  case _ => None
                }
                PhoneNumber(number.toLong, nationalFormat, tollFree, region, status, lease, numberConfiguration)
              }
              case _ => deserializationError("Failure deserializing PhoneNumber because required fields Number, NationalFormat, and TollFree were not all present")
            }
          }).toSeq
        }
        case numberJson: JsObject => {
          numberJson.getFields("Number", "NationalFormat", "TollFree") match {
            case Seq(JsNumber(number), JsString(nationalFormat), JsBoolean(tollFree)) => {
              val region: Option[Region] = numberJson.fields.get("Region") match {
                case Some(regionJson) => Option(regionJson.convertTo[Region])
                case _ => None
              }
              val status: Option[PhoneNumberStatus] = numberJson.fields.get("Status") match {
                case Some(JsString(value)) => Option(PhoneNumberStatus.withName(value))
                case _ => None
              }
              val lease: Option[Lease] = numberJson.fields.get("LeaseInfo") match {
                case Some(leaseJson) => Option(leaseJson.convertTo[Lease])
                case _ => None
              }
              val numberConfiguration: Option[PhoneNumberConfiguration] = numberJson.fields.get("NumberConfiguration") match {
                case Some(numberConfigurationJson) => Option(numberConfigurationJson.convertTo[PhoneNumberConfiguration])
                case _ => None
              }
              Seq(PhoneNumber(number.toLong, nationalFormat, tollFree, region, status, lease, numberConfiguration))
            }
            case _ => deserializationError("Failure deserializing PhoneNumber because required fields Number, NationalFormat, and TollFree were not all present")
          }
        }
        case _ => deserializationError("Exepecting JSON array or JSON object (for single result)")
      }
    }

    def read(json: JsValue): Seq[PhoneNumber] = json match {
      case jsonResponse: JsObject => {
        val resourceListJson = jsonResponse.getFields("ResourceList").head.asJsObject
        resourceListJson.getFields("@totalResults").head match {
          case JsString(totalResultsString) => {
            val totalResults = totalResultsString.toInt
            if (totalResults <= 0) Seq.empty[PhoneNumber] else parsePhoneNumbers(resourceListJson)
          }
          case _ => deserializationError("@totalResults was not a JSON number")
        }
      }
      case _ => deserializationError("Expecting JSON object")
    }
  }

  implicit val OrderReferenceFormat = new RootJsonFormat[OrderReference] {
    def write(obj: OrderReference): JsValue = ???

    def read(json: JsValue): OrderReference = json match {
      case responseJson: JsObject => {
        responseJson.fields.get("ResourceReference") match {
          case Some(reference) => {
            reference match {
              case referenceJson: JsObject => {
                referenceJson.getFields("Id", "Location") match {
                  case Seq(JsNumber(id), JsString(location)) => OrderReference(id.toLong, Uri(location))
                  case _ => deserializationError("Failure deserializing order ResourceReference because required fields Id and Location were not all present")
                }
              }
              case default => deserializationError(s"Failed to deserialize ResourceReference. Exepecting JsObject, but got ${default.getClass}")
            }
          }
          case None => deserializationError("Failure deserializing OrderReference because there was no field ResourceReference that was present")
        }
      }
      case _ => deserializationError(s"Expecting that the response would be a json object, but it wasn't ... it was ${json.getClass}")
    }
  }

  implicit val orderStatusFormat = new RootJsonFormat[OrderStatus] {
    def write(obj: OrderStatus): JsValue = obj.name.toJson

    def read(json: JsValue): OrderStatus = json match {
      case JsString(s) => OrderStatus.withName(s)
      case default => deserializationError(s"Expecting OrderStatus to be json string, but got $default.getClass")
    }
  }

  implicit val jodaMoneyFormat = new RootJsonFormat[Money] {
    def write(obj: Money): JsValue = obj.toString.toJson

    def read(json: JsValue): Money = json match {
      case JsString(s) => Money.parse(s)
      case default => deserializationError(s"Expecting money to be json string, but got $default.getClass")
    }
  }

  implicit val jodaDateTimeFormat = new RootJsonFormat[DateTime] {
    def write(obj: DateTime): JsValue = obj.millis.toLong.toJson

    def read(json: JsValue): DateTime = json match {
      case JsNumber(value) => new DateTime(value)
      case default => deserializationError(s"Expecting DateTime to be a number, but got $default.getClass")
    }
  }

  implicit val keywordFormat = new RootJsonFormat[Keyword] {
    def write(obj: Keyword): JsValue = obj.

    def read(json: JsValue): Keyword = ???
  }

  override type JF[T] = JsonFormat[T]
  implicit val orderItemFormat = new RootJsonFormat[OrderItem[JF]] {
    def write(obj: OrderItem[ApiEntityFormats.JF]): JsValue = JsObject(Map(
      "quantity" -> obj.quantity.toJson,
      "itemCost" -> obj.itemCost.toJson,
      "itemsFulfilled" -> seqFormat[JF].write(obj.itemsFulfilled)
    ))

    def read(json: JsValue): OrderItem[ApiEntityFormats.JF] = ???
  }

  implicit val OrderFormat = new RootJsonFormat[Order] {
    def write(obj: Order): JsValue = JsObject(Map(
      "id" -> obj.id.toJson,
      "status"-> obj.status.toJson,
      "totalCost" -> obj.totalCost.toJson,
      "createdOn" -> obj.createdOn.toJson,
      "keywords" -> optionFormat[OrderItem[Keyword]].write(obj.keywords),
      "localNumbers" -> optionFormat[OrderItem[PhoneNumber]].write(obj.localNumbers),
      "tollFreeNumbers" -> optionFormat[OrderItem[PhoneNumber]].write(obj.tollFreeNumbers)
    ))

    def read(json: JsValue): Order = json match {
      case JsObject(fields) => {
        fields.get("Resource") match {
          case Some(resource) => {
            resource match {
              case JsObject(resourceJson) => {
                resourceJson.get("NumberOrder") match {
                  case Some(order) => {
                    order match {
                      case orderJson: JsObject => {
                        orderJson.getFields("@id", "Status", "Created", "TotalCost") match {
                          case Seq(JsString(id), JsString(status), JsString(createdOn), JsNumber(totalCost)) => {
                            val maybeLocalNumbers: Option[OrderItem[PhoneNumber]] = orderJson.fields.get("LocalNumbers") match {
                              case Some(localNumbersValue) => localNumbersValue match {
                                case localNumbersJson: JsObject => {
                                  localNumbersJson.getFields("Ordered", "UnitCost") match {
                                    case Seq(JsNumber(quantity), JsNumber(itemCost)) => {
                                      val fulfilled: Seq[PhoneNumber] = localNumbersJson.fields.get("Fulfilled") match {
                                        case Some(itemsJson) => itemsJson match {
                                          case JsString(itemsFulfilled) => {
                                            itemsFulfilled.split(" ").map(item => PhoneNumber(item)).toSeq
                                          }
                                          case _ => deserializationError(s"Expecting 'Fulfilled' to be json string, but got ${itemsJson.getClass}")
                                        }
                                        case None => List.empty
                                      }
                                      Some(OrderItem[PhoneNumber](quantity.toInt, Money.of(CurrencyUnit.USD, itemCost.bigDecimal), fulfilled))
                                    }
                                    case _ => deserializationError("Ordered and UnitCost are required, but were not present in the 'LocalNumbers' json")
                                  }
                                }
                                case _ => deserializationError(s"Expecting 'LocalNumbers' to be a json object, but got ${localNumbersValue.getClass}")
                              }
                              case None => None
                            }
                            val maybeTollFreeNumbers: Option[OrderItem[PhoneNumber]] = orderJson.fields.get("TollFreeNumbers") match {
                              case Some(tollfreeNumbersValue) => tollfreeNumbersValue match {
                                case tollfreeNumbersJson: JsObject => {
                                  tollfreeNumbersJson.getFields("Ordered", "UnitCost") match {
                                    case Seq(JsNumber(quantity), JsNumber(itemCost)) => {
                                      val fulfilled: Seq[PhoneNumber] = tollfreeNumbersJson.fields.get("Fulfilled") match {
                                        case Some(itemsJson) => itemsJson match {
                                          case JsString(itemsFulfilled) => {
                                            itemsFulfilled.split(" ").map(item => PhoneNumber(item)).toSeq
                                          }
                                          case _ => deserializationError(s"Expecting 'Fulfilled' to be json string, but got ${itemsJson.getClass}")
                                        }
                                        case None => List.empty
                                      }
                                      Some(OrderItem[PhoneNumber](quantity.toInt, Money.of(CurrencyUnit.USD, itemCost.bigDecimal), fulfilled))
                                    }
                                    case _ => deserializationError("Ordered and UnitCost are required, but were not present in the 'TollFreeNumbers' json")
                                  }
                                }
                                case _ => deserializationError(s"Expecting 'TollFreeNumbers' to be a json object, but got ${tollfreeNumbersValue.getClass}")
                              }
                              case None => None
                            }
                            val maybeKeywords: Option[OrderItem[Keyword]] = orderJson.fields.get("Keywords") match {
                              case Some(keywordsValue) => keywordsValue match {
                                case keywordsJson: JsObject => {
                                  keywordsJson.getFields("Ordered", "UnitCost") match {
                                    case Seq(JsNumber(quantity), JsNumber(itemCost)) => {
                                      val fulfilled: Seq[Keyword] = keywordsJson.fields.get("Fulfilled") match {
                                        case Some(itemsJson) => itemsJson match {
                                          case JsString(itemsFulfilled) => {
                                            itemsFulfilled.split(" ").map(Keyword(_)).toSeq
                                          }
                                          case _ => deserializationError(s"Expecting 'Fulfilled' to be json string, but got ${itemsJson.getClass}")
                                        }
                                        case None => List.empty
                                      }
                                      Some(OrderItem[Keyword](quantity.toInt, Money.of(CurrencyUnit.USD, itemCost.bigDecimal), fulfilled))
                                    }
                                    case _ => deserializationError("Ordered and UnitCost are required, but were not present in the 'Keywords' json")
                                  }
                                }
                                case _ => deserializationError(s"Expecting 'Keywords' to be a json object, but got ${keywordsValue.getClass}")
                              }
                              case None => None
                            }

                            Order(id.toLong, OrderStatus.withName(status), CallFireIsoDateTimeFormatter.parseDateTime(createdOn), Money.of(CurrencyUnit.USD, totalCost.bigDecimal), maybeLocalNumbers, maybeTollFreeNumbers, maybeKeywords)
                          }
                          case _ => deserializationError("Failed to get required fields '@id, Status, Created, and TotalCost'")
                        }
                      }
                      case _ => deserializationError(s"Expecting 'NumberOrder' to be json object, but got ${order.getClass}")
                    }
                  }
                  case _ => deserializationError("Failed to get required field 'NumberOrder'")
                }
              }
              case _ => deserializationError(s"Expecting 'Resource' to be a json object, but was ${resource.getClass}")
            }
          }
          case _ => deserializationError("Failed to get required field 'Resource'")
        }
      }
      case _ => deserializationError(s"Expecting that the response woudl be a json object, but it was ${json.getClass}")
    }
  }

//  implicit val SearchNumbersResultFormat = new RootJsonFormat[SearchNumbersResult] {
//    def write(obj: SearchNumbersResult): JsValue = ???
//
//    def read(json: JsValue): SearchNumbersResult = json match {
//      case listObject: JsonObject
//    }
//  }
}
