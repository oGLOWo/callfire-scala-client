package com.oglowo.callfire.entity

import spray.http.Uri

case class SoundReference(override val id: Long, override val location: Uri) extends ApiEntityReference
