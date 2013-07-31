package com.oglowo.callfire.entity

import scala.xml.NodeSeq

case class InboundIvrConfiguration(dialplanXml: Option[NodeSeq], id: Option[Long] = None) extends InboundCallConfiguration[IvrConfigurationType.type] {
  def inboundCallConfigurationType = IvrConfigurationType
}
