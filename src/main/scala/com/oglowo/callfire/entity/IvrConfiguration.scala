package com.oglowo.callfire.entity

import InboundCallConfigurationType._
import scala.xml.NodeSeq

case class IvrConfiguration(dialplanXml: Option[NodeSeq]) extends InboundCallConfiguration[Ivr.type] {
  def inboundCallConfigurationType = Ivr
}
