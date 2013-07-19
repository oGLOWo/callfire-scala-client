package com.oglowo.callfire.entity

import InboundCallConfigurationType._
import scala.xml.NodeSeq

case class InboundIvrConfiguration(dialplanXml: Option[NodeSeq], id: Option[Long] = None) extends InboundCallConfiguration[Ivr.type] {
  def inboundCallConfigurationType: InboundCallConfigurationType = Ivr
}
