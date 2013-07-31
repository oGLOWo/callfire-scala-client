package com.oglowo.callfire.entity

case class CallTrackingConfiguration(transferNumbers: Set[String] = Set.empty, screen: Boolean = false, record: Boolean = false, introductionSoundId: Option[Long] = None, whisperSoundId: Option[Long] = None, id: Option[Long] = None) extends InboundCallConfiguration[CallTrackingConfigurationType.type] {
  def inboundCallConfigurationType = CallTrackingConfigurationType
}
