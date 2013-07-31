package com.oglowo.callfire.entity

trait InboundCallConfiguration[T <: InboundCallConfigurationType] {
  def inboundCallConfigurationType: T
  def id: Option[Long]
}
