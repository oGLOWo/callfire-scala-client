package com.oglowo.callfire.entity

case class Keyword(shortCode: String, keyword: String, status: Option[PhoneNumberStatus] = None, lease: Option[Lease] = None) extends ApiEntity
object Keyword {
  def apply(keyword: String): Keyword = Keyword("", keyword)
}