package com.oglowo.callfire.entity

import java.util.{Date, UUID}
import PhoneNumberStatus._
import scalaz._
import Scalaz._
import com.oglowo.callfire.Imports._

case class PhoneNumber(number: Long,
                       nationalFormat: String,
                       tollFree: Boolean,
                       region: Option[Region] = None,
                       status: Option[PhoneNumberStatus] = None,
                       lease: Option[Lease] = None,
                       configuration: Option[PhoneNumberConfiguration] = None) extends ApiEntity

object PhoneNumber {
  val TollFreePrefixes = Set(
    TollFreePrefix("800", true, "est. 1967".some),
    TollFreePrefix("888", true, "est. 1996".some),
    TollFreePrefix("877", true, "est. 1998".some),
    TollFreePrefix("866", true, "est. 2000".some),
    TollFreePrefix("855", true, "est. 2010".some),
    TollFreePrefix("844", false, "tbe Dec 7, 2013".some),
    TollFreePrefix("833", false, "reserved for future expansion".some),
    TollFreePrefix("822", false, "reserved for future expansion".some),
    TollFreePrefix("880", false, "reserved for future expansion".some),
    TollFreePrefix("881", false, "reserved for future expansion".some),
    TollFreePrefix("882", false, "reserved for future expansion".some),
    TollFreePrefix("883", false, "reserved for future expansion".some),
    TollFreePrefix("884", false, "reserved for future expansion".some),
    TollFreePrefix("885", false, "reserved for future expansion".some),
    TollFreePrefix("886", false, "reserved for future expansion".some),
    TollFreePrefix("887", false, "reserved for future expansion".some),
    TollFreePrefix("889", false, "reserved for future expansion".some)
  )

  def apply(number: Long): PhoneNumber = PhoneNumber(number, "", false, None, None, None, None)
  def apply(number: MaxString15): PhoneNumber = PhoneNumber(implicitly[String](number).toLong, "", false, None, None, None, None)
}

