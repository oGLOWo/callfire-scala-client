package com.oglowo.callfire.entity

import spray.http.Uri

case class TextBroadcastReference(override val id: Long, override val location: Uri) extends ApiEntityReference
