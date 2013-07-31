package com.oglowo.callfire.json

import spray.json._
import com.oglowo.callfire.entity.{PhoneNumber, ApiError}

object ApiEntityFormats extends DefaultJsonProtocol {
  implicit val apiErrorFormat = new RootJsonFormat[ApiError] {
    def write(obj: ApiError): JsValue = ???

    def read(json: JsValue): ApiError = json match {
      case jsonResponse: JsObject => {
        jsonResponse.getFields("ResourceException").headOption
        throw new DeserializationException("JSON did not contain ResourceException field")


      }

      case default => throw new DeserializationException(s"Expected JsObject, but got ${default.getClass}")
    }
  }

  implicit val phoneNumberFormat = new RootJsonFormat[PhoneNumber] {
    def write(obj: PhoneNumber): JsValue = ???

    def read(json: JsValue): PhoneNumber = json match {
      case jsonResponse: JsObject => {
        val resourceWrapper = jsonResponse.getFields("Resource").head.asJsObject
        val entityWrapper = resourceWrapper.getFields("Number").head.asJsObject
        val fields = entityWrapper.getFields("Number", "NationalFormat", "TollFree", "Region", "Status", "LeaseInfo", "NumberConfiguration")
        fields match {
          case Seq(JsString(number), JsString(nationalFormat), JsBoolean(tollFree), JsObject(regionObject), JsString(statusValue), JsObject(leaseObject), JsObject(numberConfigurationObject)) => {
            null
          }
          case _ => deserializationError("Failure deserializing PhoneNumber")
        }
      }
      case _ => deserializationError("Expecting JSON object")
    }
  }
}
