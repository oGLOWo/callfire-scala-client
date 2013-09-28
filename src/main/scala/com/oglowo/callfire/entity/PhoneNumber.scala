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
  val TollFreeCodes: Set[String] = Set("800", "888", "877", "866", "855")

  def apply(number: Long): PhoneNumber = PhoneNumber(number, "", false, None, None, None, None)
  def apply(number: MaxString15): PhoneNumber = PhoneNumber(implicitly[String](number).toLong, "", false, None, None, None, None)
}

