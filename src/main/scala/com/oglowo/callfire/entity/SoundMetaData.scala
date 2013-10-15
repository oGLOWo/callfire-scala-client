package com.oglowo.callfire.entity

import org.joda.time.DateTime
import scala.concurrent.duration.Duration

case class SoundMetaData(id: Long, status: SoundStatus, name: String, createdOn: DateTime, duration: Option[Duration] = None)