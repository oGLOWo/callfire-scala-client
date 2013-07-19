package com.oglowo.callfire.entity

object PhoneNumberStatus extends Enumeration {
  type PhoneNumberStatus = Value
  val Pending     = Value("PENDING")
  val Active      = Value("ACTIVE")
  val Released    = Value("RELEASED")
  val Unavailable = Value("UNAVAILABLE")
}