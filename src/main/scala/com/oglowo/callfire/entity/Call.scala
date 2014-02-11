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
                finalResult: Option[Result] = None,
                callRecords: Set[CallRecord] = Set.empty) extends ApiEntity {
  def billedDuration: Duration = (0.seconds.asInstanceOf[Duration] /: callRecords)(_ + _.billedDuration)

  def billedDuration(p: (CallRecord) => Boolean): Duration = (0.seconds.asInstanceOf[Duration] /: callRecords.filter(p))(_ + _.billedDuration)

  def voicemailSoundName: Option[String] = {
    callRecords.find(_.containsVoicemail) match {
      case Some(callRecord) => Some(callRecord.recordingsMetaData.head.name)
      case None => None
    }
  }

  def voicemailRecordingId: Option[Long] = {

    callRecords.find(_.containsVoicemail) match {
      case Some(callRecord) => Some(callRecord.recordingsMetaData.head.id)
      case None => None
    }
  }

  def voicemailRecording: Option[RecordingMeta] = {
    callRecords.find(_.containsVoicemail) match {
      case Some(callRecord) => Some(callRecord.recordingsMetaData.head)
      case None => None
    }
  }
}

