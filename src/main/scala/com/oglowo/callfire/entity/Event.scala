package com.oglowo.callfire.entity

trait Event extends ApiEntity {
  val subscriptionId: Long
}

trait EventCompanion[_ <: Event] {
  val CallFireName: String
}