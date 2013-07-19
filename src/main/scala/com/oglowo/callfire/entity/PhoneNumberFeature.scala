package com.oglowo.callfire.entity

object PhoneNumberFeature extends Enumeration {
  type PhoneNumberFeature = Value
  val Unsupported = Value("UNSUPPORTED")
  val Pending     = Value("PENDING")
  val Disabled    = Value("DISABLED")
  val Enabled     = Value("ENABLED")
}
