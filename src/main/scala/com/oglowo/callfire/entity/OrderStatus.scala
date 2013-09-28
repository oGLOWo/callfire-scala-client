package com.oglowo.callfire.entity

import com.oglowo.callfire.{BetterEnumerationCompanion, BetterEnumeration}

sealed trait OrderStatus extends BetterEnumeration
object OrderStatus extends BetterEnumerationCompanion[OrderStatus] {
  val values: Set[OrderStatus] = Set()
}

case object NewOrderStatus extends OrderStatus { override def name = "NEW" }
case object ProcessingOrderStatus extends OrderStatus { override def name = "PROCESSING" }
case object FinishedOrderStatus extends OrderStatus { override def name = "FINISHED" }
case object ErroredOrderStatus extends OrderStatus { override def name = "ERRORED" }
case object VoidOrderStatus extends OrderStatus { override def name = "VOID" }
