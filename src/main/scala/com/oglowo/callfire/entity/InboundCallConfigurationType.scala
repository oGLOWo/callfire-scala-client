package com.oglowo.callfire.entity

object InboundCallConfigurationType extends Enumeration {
  type InboundCallConfigurationType = Value
  val Tracking = Value("TRACKING")
  val Ivr      = Value("IVR")
}
