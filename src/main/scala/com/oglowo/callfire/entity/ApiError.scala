package com.oglowo.callfire.entity

import spray.json.DefaultJsonProtocol._

case class ApiError(details: String, errorType: String)

object ApiError {
  implicit val apiErrorFormat = jsonFormat2(ApiError.apply)
}