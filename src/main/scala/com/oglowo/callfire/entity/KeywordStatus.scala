package com.oglowo.callfire.entity

import com.oglowo.callfire.{BetterEnumerationCompanion, BetterEnumeration}

sealed trait KeywordStatus extends BetterEnumeration
object KeywordStatus extends BetterEnumerationCompanion[KeywordStatus] {
  override val values: Set[KeywordStatus] = Set(PendingKeywordStatus, ActiveKeywordStatus, ReleasedKeywordStatus, UnavailableKeywordStatus)
}

case object PendingKeywordStatus extends KeywordStatus { override def name = "PENDING" }
case object ActiveKeywordStatus extends KeywordStatus { override def name = "ACTIVE" }
case object ReleasedKeywordStatus extends KeywordStatus { override def name = "RELEASED" }
case object UnavailableKeywordStatus extends KeywordStatus { override def name = "UNAVAILABLE" }
