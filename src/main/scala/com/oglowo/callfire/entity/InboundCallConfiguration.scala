package com.oglowo.callfire.entity
import InboundCallConfigurationType._

trait InboundCallConfiguration[T <: InboundCallConfigurationType] {
  def inboundCallConfigurationType: T
  def id: Option[Long]
}
