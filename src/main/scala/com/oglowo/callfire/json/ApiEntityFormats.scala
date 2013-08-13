package com.oglowo.callfire.json

import spray.json._
import com.oglowo.callfire.entity._
import com.oglowo.callfire.entity.ApiError
import com.oglowo.callfire.entity.PhoneNumber
import com.github.nscala_time.time.Imports._
import scala.xml.{XML, NodeSeq}

object ApiEntityFormats extends DefaultJsonProtocol {
  val CallFireDateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-ddZ")

  implicit val apiErrorFormat = new RootJsonFormat[ApiError] {
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

  implicit val regionFormat = new RootJsonFormat[Region] {
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

  implicit val leaseFormat = new RootJsonFormat[Lease] {
    def write(obj: Lease): JsValue = ???

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

  implicit val phoneNumberConfigurationFormat = new RootJsonFormat[PhoneNumberConfiguration] {
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

  implicit val phoneNumberFormat = new RootJsonFormat[PhoneNumber] {
    def write(obj: PhoneNumber): JsValue = ???

    def read(json: JsValue): PhoneNumber = json match {
      case jsonResponse: JsObject => {
        val resourceJson = jsonResponse.getFields("Resource").head.asJsObject
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
      case _ => deserializationError("Expecting JSON object")
    }
  }
}
