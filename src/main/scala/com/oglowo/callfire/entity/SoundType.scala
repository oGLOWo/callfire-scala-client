package com.oglowo.callfire.entity

import com.oglowo.callfire.{BetterEnumerationCompanion, BetterEnumeration}

sealed trait SoundType extends BetterEnumeration
object SoundType extends BetterEnumerationCompanion[SoundType] {
  val values: Set[SoundType] = Set(Mp3SoundType, WavSoundType)
}

case object Mp3SoundType extends SoundType
case object WavSoundType extends SoundType