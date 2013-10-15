package com.oglowo.callfire.entity

import com.oglowo.callfire.{BetterEnumerationCompanion, BetterEnumeration}

sealed trait SoundStatus extends BetterEnumeration
object SoundStatus extends BetterEnumerationCompanion[SoundStatus] {
  val values: Set[SoundStatus] = Set(PendingSoundStatus, ActiveSoundStatus, FailedSoundStatus, ArchivedSoundStatus)
}

case object PendingSoundStatus extends SoundStatus { override def name = "PENDING" }
case object ActiveSoundStatus extends SoundStatus { override def name = "ACTIVE" }
case object FailedSoundStatus extends SoundStatus { override def name = "FAILED" }
case object ArchivedSoundStatus extends SoundStatus { override def name = "ARCHIVED" }
