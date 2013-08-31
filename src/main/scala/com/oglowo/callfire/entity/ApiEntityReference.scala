package com.oglowo.callfire.entity

import spray.http.Uri

trait ApiEntityReference {
  val id: Long
  val location: Uri
}
