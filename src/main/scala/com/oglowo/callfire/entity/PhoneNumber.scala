package com.oglowo.callfire.entity

import java.util.{Date, UUID}
import PhoneNumberStatus._

case class PhoneNumber(number: String,
                       nationalFormat: String,
                       tollFree: Boolean,
                       region: Option[Region] = None,
                       status: Option[PhoneNumberStatus] = None,
                       lease: Lease,
                       configuration: PhoneNumberConfiguration) extends ApiEntity