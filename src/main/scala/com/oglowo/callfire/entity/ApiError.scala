package com.oglowo.callfire.entity

import spray.http.StatusCode

case class ApiError(message: String, httpStatus: StatusCode)
