package com.oglowo.callfire.entity

sealed trait PhoneNumberStatus {
  def value: String
}
object PhoneNumberStatus {
  val values: Seq[PhoneNumberStatus] = Seq(PendingPhoneNumberStatus, ActivePhoneNumberStatus, ReleasedPhoneNumberStatus, UnavailablePhoneNumberStatus)

  def withName(name: String): PhoneNumberStatus = values.find(_.value == name).get
}

case object PendingPhoneNumberStatus extends PhoneNumberStatus {
  def value = "PENDING"
}

case object ActivePhoneNumberStatus extends PhoneNumberStatus {
  def value = "ACTIVE"
}

case object ReleasedPhoneNumberStatus extends PhoneNumberStatus {
  def value = "RELEASED"
}

case object UnavailablePhoneNumberStatus extends PhoneNumberStatus {
  def value = "UNAVAILABLE"
}
