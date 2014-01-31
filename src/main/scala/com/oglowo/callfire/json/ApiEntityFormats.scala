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
import com.typesafe.scalalogging.log4j.Logging
import scala.concurrent.duration.{Duration => ScalaDuration}
import java.util.concurrent.TimeUnit

object ApiEntityFormats extends DefaultJsonProtocol with Logging {
  val CallFireDateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-ddZ")
  val CallFireIsoDateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis

  implicit val UriFormat = new RootJsonFormat[Uri] {
    def write(obj: Uri): JsValue = Option(obj) match {
      case Some(uri) => uri.toString.toJson
      case None => serializationError("Cannot serialize a null Uri object to json")
    }

    def read(json: JsValue): Uri = json match {
      case JsString(value) => Uri(value)
      case _ => deserializationError(s"Expecting uri to be a json string, but it was ${json.getClass}")
    }
  }

  implicit val ApiErrorFormat = new RootJsonFormat[ApiError] {
    def write(obj: ApiError): JsValue = ???

    def read(json: JsValue): ApiError = json match {
      case jsonResponse: JsObject => {
        jsonResponse.getFields("ResourceException").headOption match {
          case Some(exceptionJson) => {
            exceptionJson.asJsObject.getFields("HttpStatus", "Message") match {
              case Seq(JsNumber(status), JsString(message)) => ApiError(message, status.intValue)
              case _ => deserializationError("JSON did not contain required fields HttpStatus and Message")
            }
          }
          case _ => deserializationError("JSON did not contain ResourceException field")
        }
      }

      case default => deserializationError(s"Expected JsObject, but got ${default.getClass}")
    }
  }

  implicit val SoundReferenceFormat = new RootJsonFormat[SoundReference] {
    def write(obj: SoundReference): JsValue = Option(obj) match {
      case Some(reference) => JsObject(Map(
        "id" -> reference.id.toJson,
        "location" -> reference.location.toJson
      ))
      case None => serializationError("null object cannot be serialized as SoundReference")
    }

    def read(json: JsValue): SoundReference = json match {
      case responseJson: JsObject => {
        responseJson.fields.get("ResourceReference") match {
          case Some(reference) => {
            reference match {
              case referenceJson: JsObject => {
                referenceJson.getFields("Id", "Location") match {
                  case Seq(JsNumber(id), JsString(location)) => SoundReference(id.toLong, Uri(location))
                  case _ => deserializationError("Failure deserializing order ResourceReference because required fields Id and Location were not all present")
                }
              }
              case default => deserializationError(s"Failed to deserialize ResourceReference. Exepecting JsObject, but got ${default.getClass}")
            }
          }
          case None => deserializationError("Failure deserializing SoundReference because there was no field ResourceReference that was present")
        }
      }
      case _ => deserializationError(s"Expecting that the response would be a json object, but it wasn't ... it was ${json.getClass}")
    }
  }

  implicit val SoundMetaDataFormat = new RootJsonFormat[SoundMetaData] {
    def write(obj: SoundMetaData): JsValue = ???

    def read(json: JsValue): SoundMetaData = json match {
      case responseJson@JsObject(responseFields) => {
        responseFields.get("Resource") match {
          case Some(resource) => resource match {
            case resourceJson@JsObject(resourceFields) => resourceFields.get("SoundMeta") match {
              case Some(meta) => meta match {
                case metaJson@JsObject(metaFields) => metaJson.getFields("@id", "Status", "Name", "Created", "LengthInSeconds") match {
                  case Seq(JsNumber(id), JsString(status), JsString(name), JsString(createdOn), JsNumber(lengthInSeconds)) => {
                    SoundMetaData(id.toLong, SoundStatus.withName(status), name, CallFireIsoDateTimeFormatter.parseDateTime(createdOn), Some(ScalaDuration(lengthInSeconds.toLong, TimeUnit.SECONDS)))
                  }
                  case Seq(JsNumber(id), JsString(status), JsString(name), JsString(createdOn)) => {
                    SoundMetaData(id.toLong, SoundStatus.withName(status), name, CallFireIsoDateTimeFormatter.parseDateTime(createdOn))
                  }
                  case _ => deserializationError("Failure deserializing SoundMeta because it was missing one of the required @id, Status, Name, and/org Created fields")
                }
                case _ => deserializationError(s"Expecting SoundMeta to be json object, but it was ${meta.getClass}")
              }
              case None => deserializationError("Failure deserializing SoundMetaData because it was missing required SoundMeta field")
            }
            case _ => deserializationError(s"Expecting Resource to be json object, but it was ${resource.getClass}")
          }
          case None => deserializationError("Failure deserializing SoundMetaData because it was missing the requied Resource field")
        }
      }
      case _ => deserializationError(s"Expecting response to be json object, but it was ${json.getClass}")
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

        if (inboundCallConfigurationType.isDefined) {
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
        }}
        else {
          PhoneNumberConfiguration(callFeature, textFeature, None)
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

  implicit val keywordStatusFormat = new RootJsonFormat[KeywordStatus] {
    def write(obj: KeywordStatus): JsValue = obj.name.toJson

    def read(json: JsValue): KeywordStatus = json match {
      case JsString(value) => KeywordStatus.withName(value)
      case default => deserializationError(s"Expecting KeywordStatus to be json string, but got ${default.getClass}")
    }
  }

  implicit val PhoneNumberFormat = new RootJsonFormat[PhoneNumber] {
    def write(obj: PhoneNumber): JsValue = JsObject(Map(
      "configuration" -> obj.configuration.toJson,
      "lease" -> obj.lease.toJson,
      "nationalFormat" -> obj.nationalFormat.toJson,
      "number" -> obj.number.toJson,
      "region" -> obj.region.toJson,
      "status" -> obj.status.toJson,
      "tollFree" -> obj.tollFree.toJson
    ))

    def parsePhoneNumber(resourceJson: JsObject): PhoneNumber = {
      logger.debug("Here is the json that we get: " + resourceJson)
      val entityJson = resourceJson.getFields("Number").head.asJsObject
      val fields = entityJson.getFields("Number", "NationalFormat", "TollFree")
      fields match {
        case Seq(JsString(number), JsString(nationalFormat), JsBoolean(tollFree)) => {
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
          PhoneNumber(number.toLong, nationalFormat, region, status, lease, numberConfiguration)
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
            println(">>>>> Here are the fields " + fields)
            fields match {
              case Seq(JsString(number), JsString(nationalFormat), JsBoolean(tollFree)) => {
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
                PhoneNumber(number.toLong, nationalFormat, region, status, lease, numberConfiguration)
              }
              case _ => deserializationError("Failure deserializing PhoneNumber because required fields Number, NationalFormat, and TollFree were not all present")
            }
          }).toSeq
        }
        case numberJson: JsObject => {
          numberJson.getFields("Number", "NationalFormat", "TollFree") match {
            case Seq(JsString(number), JsString(nationalFormat), JsBoolean(tollFree)) => {
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
              Seq(PhoneNumber(number.toLong, nationalFormat, region, status, lease, numberConfiguration))
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
          case JsNumber(number) => {
            val totalResults = number.toInt
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

  implicit val keywordFormat = new RootJsonFormat[Keyword] {
    def write(obj: Keyword): JsValue = JsObject(Map(
      "shortCode" -> obj.shortCode.toJson,
      "keyword" -> obj.keyword.toJson,
      "status" -> optionFormat[KeywordStatus].write(obj.status),
      "lease" -> optionFormat[Lease].write(obj.lease)
    ))

    def read(json: JsValue): Keyword = ???
  }

  override type JF[T] = JsonFormat[T]
  implicit def orderItemFormat[T <: ApiEntity : JF] = new JF[OrderItem[T]] {
    def write(obj: OrderItem[T]): JsValue = JsObject(Map(
      "quantity" -> obj.quantity.toJson,
      "itemCost" -> obj.itemCost.toJson,
      "itemsFulfilled" -> seqFormat[T].write(obj.itemsFulfilled)
    ))

    def read(json: JsValue): OrderItem[T] = ???
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
                          case Seq(JsNumber(id), JsString(status), JsString(createdOn), JsNumber(totalCost)) => {
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

  implicit val RecordingMetaFormat = new RootJsonFormat[RecordingMeta] {
    def write(obj: RecordingMeta): JsValue = ???

    // Another CallFire bug
    def read(json: JsValue): RecordingMeta = json match {
      case JsObject(fields) => {
        val id = fields.get("@id") match {
          case Some(JsNumber(n)) => n.toLong
          case Some(JsString(s)) => s.toLong
          case None => deserializationError("@id was not present in the RecordingMeta object")
        }

        val name = fields.get("Name") match {
          case Some(JsString(s)) => s
          case None => deserializationError("Name was not present in the RecordingMeta object")
        }

        val createdOn = fields.get("Created") match {
          case Some(JsString(s)) => CallFireIsoDateTimeFormatter.parseDateTime(s)
          case None => deserializationError("Created was not present in the RecordingMeta object")
        }

        val lengthInSeconds = fields.get("LengthInSeconds") match {
          case Some(JsNumber(n)) => n.toInt
          case None => deserializationError("LengthInSeconds was not present in the RecordingMeta object")
        }

        val link = fields.get("Link") match {
          case Some(JsString(s)) => Uri(s)
          case None => deserializationError("Link was not present in the RecordingMeta object")
        }

        RecordingMeta(id, name, createdOn, link, Some(lengthInSeconds))
      }
      case default => deserializationError(s"Expecting RecordingMeta to be json object, but got ${default.getClass.getName}")
    }
  }

  implicit val RecordingMetaSetFormat = new RootJsonFormat[Set[RecordingMeta]] {
    def write(obj: Set[RecordingMeta]): JsValue = ???

    def read(json: JsValue): Set[RecordingMeta] = immSetFormat[RecordingMeta].read(json)
  }

  implicit val CallRecordFormat = new RootJsonFormat[CallRecord] {
    def write(obj: CallRecord): JsValue = ???

    def read(json: JsValue): CallRecord = json match {
      case JsObject(fields) => {
        val id = fields.get("@id") match {
          case Some(JsNumber(n)) => n.toLong
          case Some(JsString(s)) => s.toLong
          case None => deserializationError("@id was not present in CallRecord object")
        }

        val result = fields.get("Result") match {
          case Some(JsString(s)) => Result.withName(s)
          case None => deserializationError("Result was not present in the CallRecord object")
        }

        val finishedOn = fields.get("FinishTime") match {
          case Some(JsString(s)) => CallFireIsoDateTimeFormatter.parseDateTime(s)
          case None => deserializationError("FinishTime was not present in the CallRecord object")
        }

        val billedAmount = fields.get("BilledAmount") match {
          case Some(JsNumber(n)) => n.toDouble
          case None => deserializationError("BilledAmount was not present in the CallRecord object")
        }

        val answeredOn = fields.get("AnswerTime") match {
          case Some(JsString(s)) => CallFireIsoDateTimeFormatter.parseDateTime(s)
          case None => deserializationError("AnswerTime was not present in the CallRecord object")
        }

        val durationInSeconds = fields.get("Duration") match {
          case Some(JsNumber(n)) => n.toInt
          case None => deserializationError("Duration was not present in the CallRecord object")
        }

        // Another Callfire bug here
        val recordingsMetaData = fields.get("RecordingMeta") match {
          case Some(s) => s match {
            case metaArray : JsArray => metaArray.convertTo[Set[RecordingMeta]]
            case metaObject : JsObject => Set(metaObject.convertTo[RecordingMeta])
          }
          case None => Set.empty[RecordingMeta]
        }

        CallRecord(id, result, finishedOn, billedAmount, answeredOn, durationInSeconds, recordingsMetaData)
      }
      case default => deserializationError(s"Expecting CallRecord to be json object, but got ${default.getClass.getName}")
    }
  }

  implicit val CallRecordSetFormat = new RootJsonFormat[Set[CallRecord]] {
    def write(obj: Set[CallRecord]): JsValue = ???

    def read(json: JsValue): Set[CallRecord] = immSetFormat[CallRecord].read(json)
  }

  implicit val CallFormat = new RootJsonFormat[Call] {
    def write(obj: Call): JsValue = ???

    def stripResource(json: JsObject, wrappedObjectKey: String): JsObject = {
      json.fields.get("Resource") match {
        case Some(JsObject(resourceFields)) => resourceFields.get(wrappedObjectKey) match {
          case Some(JsObject(fields)) => JsObject(Map(wrappedObjectKey -> JsObject(fields)))
          case None => deserializationError(s"$wrappedObjectKey was not present in the resource json")
        }
        case None => json.fields.get(wrappedObjectKey) match {
          case Some(JsObject(fields)) => JsObject(Map(wrappedObjectKey -> JsObject(fields)))
          case None => deserializationError(s"$wrappedObjectKey was not present in the json")
        }
      }
    }

    def read(json: JsValue): Call = stripResource(json.asJsObject("Exepcting Call to be json object"), "Call") match {
      case JsObject(resourceFields) => resourceFields.get("Call") match {
        case Some(JsObject(callFields)) => {
          logger.debug("******************** " + json)
          // CallFire needs to fix this ... their webhooks post and @id that is a string, but
          // the regular Call format has @id as a long
          // The same thing applies to numbers like to and from
          val id = callFields.get("@id") match {
            case Some(JsNumber(n)) => n.toLong
            case Some(JsString(s)) => s.toLong
            case None => deserializationError("@id was not present in the call object")
          }

          val from = callFields.get("FromNumber") match {
            case Some(JsString(s)) => PhoneNumber(s)
            case Some(JsNumber(n)) => PhoneNumber(n.toString)
            case Some(JsObject(fields)) => fields.get("value") match {
              case Some(JsString(s)) => PhoneNumber(s)
              case None => deserializationError("ToNumber had object format, but did not contain the value field")
            }
            case None => deserializationError("FromNumber was not present in the call object")
          }

          val to = callFields.get("ToNumber") match {
            case Some(JsString(s)) => PhoneNumber(s)
            case Some(JsNumber(n)) => PhoneNumber(n.toString)
            case Some(JsObject(fields)) => fields.get("value") match {
              case Some(JsString(s)) => PhoneNumber(s)
              case None => deserializationError("ToNumber had object format, but did not contain the value field")
            }
            case None => deserializationError("ToNumber was not present in the call object")
          }

          val state = callFields.get("State") match {
            case Some(JsString(s)) => ActionState.withName(s)
            case None => deserializationError("State was not present in the call object")
          }

          val contactId = callFields.get("ContactId") match {
            case Some(JsNumber(n)) => n.toLong
            case None => deserializationError("ContactId was not present in the call object")
          }

          val inbound = callFields.get("Inbound") match {
            case Some(JsBoolean(b)) => b
            case None => deserializationError("Inbound was not present in the call object")
          }

          val createdOn = callFields.get("Created") match {
            case Some(JsString(s)) => CallFireIsoDateTimeFormatter.parseDateTime(s)
            case None => deserializationError("Created was not present in the call object")
          }

          val modifiedOn = callFields.get("Modified") match {
            case Some(JsString(s)) => CallFireIsoDateTimeFormatter.parseDateTime(s)
            case None => deserializationError("Modified was not present in the call object")
          }

          val finalResult = callFields.get("FinalResult") match {
            case Some(JsString(s)) => Result.withName(s)
            case None => deserializationError("FinalResult was not present in the call object")
          }

          // Another CallFire bug
          val callRecords = callFields.get("CallRecord") match {
            case Some(s) => s match {
              case recordArray : JsArray => recordArray.convertTo[Set[CallRecord]]
              case recordObject : JsObject => Set(recordObject.convertTo[CallRecord])
            }
            case None => deserializationError("CallRecord was not present in the call object")
          }

          Call(id, from, to, state, contactId, inbound, createdOn, modifiedOn, Some(finalResult), callRecords)
        }
        case None => deserializationError("Call was not present in call object")
      }
      case default => deserializationError(s"Expecting call to be json object, but got ${default.getClass.getName}")
    }
  }

  implicit val CallFinishedEventFormat = new RootJsonFormat[CallFinishedEvent] {
    def write(obj: CallFinishedEvent): JsValue = ???

    def read(json: JsValue): CallFinishedEvent = json match {
      case JsObject(fields) => fields.get("CallFinished") match {
        case Some(JsObject(eventFields)) => {
          val subscriptionId = eventFields.get("SubscriptionId") match {
            case Some(JsNumber(value)) => value.toLong
            case default => deserializationError(s"Expecting SubscriptionId to be json long value, but got ${default.getClass.getName}")
          }
          val call = eventFields.get("Call") match {
            case Some(value) => JsObject("Call" -> value).convertTo[Call]
            case None => deserializationError("Call was not present in the json object")
          }
          CallFinishedEvent(subscriptionId, call)
        }
        case None => deserializationError("CallFinished was not present in the json object")
      }
      case default => deserializationError(s"Expecting CallFinishedEvent to be json object, but got ${default.getClass.getName}")
    }
  }
}
