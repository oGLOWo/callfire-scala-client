package com.oglowo.callfire.entity

import spray.http.Uri

case class SoundReference(id: Long, location: Uri) extends ApiEntityReference
