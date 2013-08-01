package com.oglowo.callfire.entity

sealed trait PhoneNumberFeature {
  def value: String
}
object PhoneNumberFeature {
  def values = Seq(UnsupportedPhoneNumberFeature, PendingPhoneNumberFeature, EnabledPhoneNumberFeature, DisabledPhoneNumberFeature)
  def withName(name: String) = values.find(_.value == name).get
}

case object UnsupportedPhoneNumberFeature extends PhoneNumberFeature { def value = "UNSUPPORTED" }
case object PendingPhoneNumberFeature extends PhoneNumberFeature { def value = "PENDING" }
case object DisabledPhoneNumberFeature extends PhoneNumberFeature { def value = "DISABLED" }
case object EnabledPhoneNumberFeature extends PhoneNumberFeature { def value = "ENABLED" }

