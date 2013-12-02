package com.oglowo.callfire.entity

import java.util.{Date, UUID}
import PhoneNumberStatus._
import scalaz._
import Scalaz._
import com.oglowo.callfire.Imports._
import com.oglowo.phonenumber.{PhoneNumber => GlowPhoneNumber, PhoneNumberFormat}

case class PhoneNumber(number: Long,
                       nationalFormat: String,
                       region: Option[Region] = None,
                       status: Option[PhoneNumberStatus] = None,
                       lease: Option[Lease] = None,
                       configuration: Option[PhoneNumberConfiguration] = None) extends ApiEntity {
  def fourDigitPrefix: String = {
    val e164NumberPrefix = GlowPhoneNumber(number.toString) match {
      case Success(s) => s.format(PhoneNumberFormat.E164).substring(1, 5)
      case Failure(error) => throw new IllegalArgumentException(error)
    }
    e164NumberPrefix
  }

  def tollFree: Boolean = {
    val e164NumberPrefix = GlowPhoneNumber(number.toString) match {
      case Success(s) => s.format(PhoneNumberFormat.E164).substring(2, 5)
      case Failure(error) => throw new IllegalArgumentException(error)
    }
    PhoneNumber.TollFreePrefixes.exists(prefix => {
      prefix.prefix == e164NumberPrefix
    })
  }
}

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

  def apply(number: Long): PhoneNumber = {
    GlowPhoneNumber(number.toString) match {
      case Success(s) => PhoneNumber(number, s.format(PhoneNumberFormat.National), None, None, None, None)
      case Failure(error) => throw new IllegalArgumentException(s"Number $number could not be parsed as a valid number", error)
    }
  }
  def apply(number: MaxString15): PhoneNumber = {
    GlowPhoneNumber(number) match {
      case Success(s) => PhoneNumber(implicitly[String](number).toLong, s.format(PhoneNumberFormat.National), None, None, None, None)
      case Failure(error) => throw new IllegalArgumentException(s"Number $number could not be parsed as a valid number", error)
    }
  }
}

