package com.oglowo.callfire.entity

sealed trait InboundCallConfigurationType {
  def value: String
}
object InboundCallConfigurationType {
  val values: Seq[InboundCallConfigurationType] = Seq(IvrConfigurationType, CallTrackingConfigurationType)
  def withName(name: String): InboundCallConfigurationType = values.find(_.value == name).get
}

case object IvrConfigurationType extends InboundCallConfigurationType { def value: String = "IVR" }
case object CallTrackingConfigurationType extends InboundCallConfigurationType { def value: String = "TRACKING" }

