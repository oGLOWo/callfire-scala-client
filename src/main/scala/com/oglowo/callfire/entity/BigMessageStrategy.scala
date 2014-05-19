package com.oglowo.callfire.entity

import com.oglowo.callfire.{BetterEnumerationCompanion, BetterEnumeration}

sealed trait BigMessageStrategy extends BetterEnumeration
object BigMessageStrategy extends BetterEnumerationCompanion[BigMessageStrategy] {
  override val values: Set[BigMessageStrategy] = Set(
    SendMultiple,
    DoNotSend,
    Trim
  )

  case object SendMultiple extends BigMessageStrategy { override val name = "SEND_MULTIPLE" }
  case object DoNotSend extends BigMessageStrategy { override val name = "DO_NOT_SEND" }
  case object Trim extends BigMessageStrategy { override val name = "TRIM" }
}
