package com.oglowo.callfire.entity

import java.util.{Date, UUID}
import PhoneNumberStatus._

case class PhoneNumber(number: Long,
                       nationalFormat: String,
                       tollFree: Boolean,
                       region: Option[Region] = None,
                       status: Option[PhoneNumberStatus] = None,
                       lease: Option[Lease] = None,
                       configuration: Option[PhoneNumberConfiguration] = None) extends ApiEntity