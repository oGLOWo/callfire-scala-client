package com.oglowo.callfire.entity

case class TextNotificationEvent(subscriptionId: Long, text: Text) extends Event

object TextNotificationEvent extends EventCompanion[TextNotificationEvent] {
  override val CallFireName = "TextNotification"
}