package com.oglowo.callfire.entity

case class CallFinishedEvent(subscriptionId: Long, call: Call) extends Event

object CallFinishedEvent extends EventCompanion[CallFinishedEvent] {
  override val CallFireName = "CallFinished"
}