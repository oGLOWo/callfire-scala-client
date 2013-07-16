package com.oglowo.callfire.json

import spray.json._
import java.util.{UUID, Date}
import com.oglowo.callfire.entity.{ApiError, User}

object ApiEntityFormats extends DefaultJsonProtocol {
  implicit val dateFormat = new JsonFormat[Date] {
    def write(obj: Date): JsValue = JsNumber(obj.getTime)

    def read(json: JsValue): Date = json match {
      case number: JsNumber => new Date(number.value.longValue)
      case _ => new Date(0)
    }
  }

  implicit val uuidFormat = new JsonFormat[UUID] {
    def write(obj: UUID): JsValue = JsString(obj.toString)

    def read(json: JsValue): UUID = json match {
      case JsString(s) => UUID.fromString(s)
      case default: JsValue => deserializationError("Expected UUID as JsString, but got " + default)
    }
  }

  implicit val userFormat = jsonFormat6(User)

}
