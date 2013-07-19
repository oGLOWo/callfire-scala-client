package com.oglowo.callfire.entity

import com.github.nscala_time.time.Imports._

case class Lease(begin: Option[DateTime] = None, end: Option[DateTime] = None, autoRenew: Boolean) extends ApiEntity