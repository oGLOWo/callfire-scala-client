package com.oglowo.callfire.entity

import org.joda.time.DateTime
import spray.http.Uri

case class RecordingMeta(id: Long, name: String, createdOn: DateTime, link: Uri, lengthInSeconds: Option[Int] = None) extends ApiEntity {

}


