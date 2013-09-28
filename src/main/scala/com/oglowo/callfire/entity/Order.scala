package com.oglowo.callfire.entity

import com.github.nscala_time.time.Imports._
import org.joda.money.Money

case class Order(id: Long,
                 status: OrderStatus,
                 createdOn: DateTime,
                 totalCost: Money,
                 localNumbers: Option[OrderItem[PhoneNumber]] = None,
                 tollFreeNumbers: Option[OrderItem[PhoneNumber]] = None,
                 keywords: Option[OrderItem[Keyword]] = None) extends ApiEntity
