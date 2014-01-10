package com.oglowo.callfire.entity

import org.joda.time.DateTime
import scala.concurrent.duration._

case class Call(id: Long,
                from: PhoneNumber,
                to: PhoneNumber,
                state: ActionState,
                contactId: Long,
                inbound: Boolean,
                createdOn: DateTime,
                modifiedOn: DateTime,
                finalResult:
                Option[Result] = None,
                callRecords: Set[CallRecord] = Set.empty) extends ApiEntity {
  def billedDuration: Duration = (0.seconds.asInstanceOf[Duration] /: callRecords)(_ + _.billedDuration)

  def billedDuration(p: (CallRecord) => Boolean): Duration = (0.seconds.asInstanceOf[Duration] /: callRecords.filter(p))(_ + _.billedDuration)

  def voicemailSoundName: Option[String] = {
    callRecords.isEmpty match {
      case false => callRecords.head.recordingsMetaData.isEmpty match {
        case false => Some(callRecords.head.recordingsMetaData.head.name)
        case true => None
      }
      case true => None
    }
  }

  def voicemailRecordingId: Option[Long] = {
    callRecords.isEmpty match {
      case false => callRecords.head.recordingsMetaData.isEmpty match {
        case false => Some(callRecords.head.recordingsMetaData.head.id)
        case true => None
      }
      case true => None
    }
  }

  def voicemailRecording: Option[RecordingMeta] = {
    callRecords.headOption match {
      case Some(callRecord) => callRecord.recordingsMetaData.headOption match {
        case Some(recordingMeta) => Some(recordingMeta)
        case None => None
      }
      case None => None
    }
  }
}

