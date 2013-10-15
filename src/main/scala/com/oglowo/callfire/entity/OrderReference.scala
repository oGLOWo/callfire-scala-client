package com.oglowo.callfire.entity

import spray.http.Uri

case class OrderReference(id: Long, location: Uri) extends ApiEntityReference
