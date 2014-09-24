package com.oglowo.callfire.entity

import com.oglowo.callfire._
import org.joda.time.DateTime
import scala.concurrent.duration._
import scalaz._
import Scalaz._

case class Call(id: Long,
                from: Option[PhoneNumber],
                to: PhoneNumber,
                state: ActionState,
                contactId: Long,
                inbound: Boolean,
                createdOn: DateTime,
                modifiedOn: DateTime,
                finalResult: Option[Result] = None,
                callRecords: Set[CallRecord] = Set.empty) extends ApiEntity {
  def billedDuration: Duration = (0.seconds.asInstanceOf[Duration] /: callRecords)(_ + _.billedDuration)

  def billedDuration(p: (CallRecord) => Boolean): Duration = (0.seconds.asInstanceOf[Duration] /: callRecords.filter(p))(_ + _.billedDuration)

  def duration: Duration = (0.seconds.asInstanceOf[Duration] /: callRecords)(_ + _.duration)

  def duration(p: (CallRecord) => Boolean): Duration = (0.seconds.asInstanceOf[Duration] /: callRecords.filter(p))(_ + _.duration)

  def containsRecording() = callRecords.exists(_.recordingsMetaData.nonEmpty)

  def containsRecording(recordingName: String) = callRecords.exists(_.containsRecording(recordingName))

  def recordingId(recordingName: String): Option[Long] = {
    for {
      record <- callRecords.find(_.containsRecording(recordingName))
      recording <- record.recording(recordingName)
    } yield recording.id
  }

  def recording(recordingName: String): Option[RecordingMeta] = {
    for {
      record <- callRecords.find(_.containsRecording(recordingName))
      recording <- record.recording(recordingName)
    } yield recording
  }

  def containsVoicemail(recordingName: String = DefaultVoicemailRecordingName): Boolean = containsRecording(recordingName)

  def voicemailRecordingId(recordingName: String = DefaultVoicemailRecordingName): Option[Long] = recordingId(recordingName)

  def voicemailRecording(recordingName: String = DefaultVoicemailRecordingName): Option[RecordingMeta] = recording(recordingName)

  def containsCallRecording(recordingName: String = DefaultCallRecordingName): Boolean = containsRecording(recordingName)

  def callRecordingId(recordingName: String = DefaultCallRecordingName): Option[Long] = recordingId(recordingName)

  def callRecording(recordingName: String = DefaultCallRecordingName): Option[RecordingMeta] = recording(recordingName)
}

