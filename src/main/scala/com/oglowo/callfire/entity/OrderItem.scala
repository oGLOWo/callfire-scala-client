package com.oglowo.callfire.entity

import org.joda.money.Money

case class OrderItem[T <: ApiEntity](quantity: Int, itemCost: Money, itemsFulfilled: Seq[T] = Seq.empty) extends ApiEntity