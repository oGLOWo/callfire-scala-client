package com.oglowo.callfire.entity

import com.oglowo.callfire.{BetterEnumerationCompanion, BetterEnumeration}

sealed trait PhoneNumberStatus extends BetterEnumeration
object PhoneNumberStatus extends BetterEnumerationCompanion[PhoneNumberStatus] {
  override val values: Set[PhoneNumberStatus] = Set(PendingPhoneNumberStatus, ActivePhoneNumberStatus, ReleasedPhoneNumberStatus, UnavailablePhoneNumberStatus)
}

case object PendingPhoneNumberStatus extends PhoneNumberStatus { override def name = "PENDING" }
case object ActivePhoneNumberStatus extends PhoneNumberStatus { override def name = "ACTIVE" }
case object ReleasedPhoneNumberStatus extends PhoneNumberStatus { override def name = "RELEASED" }
case object UnavailablePhoneNumberStatus extends PhoneNumberStatus { override def name = "UNAVAILABLE" }
