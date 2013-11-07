package com.oglowo.callfire.entity

import org.joda.time.DateTime

case class RecordingMeta(id: Long, name: String, createdOn: DateTime, lengthInSeconds: Option[Int] = None) extends ApiEntity {

}


