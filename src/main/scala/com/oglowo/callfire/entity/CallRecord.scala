package com.oglowo.callfire.entity

import org.joda.money.Money
import org.joda.time.DateTime
import scala.math._
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

// TODO refactor this so there is an ActionRecord base
case class CallRecord(id: Long, result: Result, finishedOn: DateTime, billedAmount: Double, answeredOn: DateTime, durationInSeconds: Int, recordingsMetaData: Set[RecordingMeta] = Set.empty) extends ApiEntity {
  def billedDuration: Duration = (durationInSeconds.toDouble / 1.minute.toSeconds).ceil.minutes
  def duration: Duration = Duration(durationInSeconds.toLong, TimeUnit.SECONDS)
  def isBillable: Boolean = billedAmount > 0
  def containsRecording(recordingName: String): Boolean = recordingsMetaData.exists(_.name == recordingName)
  def recording(recordingName: String): Option[RecordingMeta] = recordingsMetaData.find(_.name == recordingName)
}

