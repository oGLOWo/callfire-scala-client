package com.oglowo.callfire.entity
import InboundCallConfigurationType._

trait InboundCallConfiguration[T <: InboundCallConfigurationType.Value] {
  def inboundCallConfigurationType: T
}
