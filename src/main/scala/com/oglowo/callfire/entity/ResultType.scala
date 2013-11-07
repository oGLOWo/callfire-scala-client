package com.oglowo.callfire.entity

import com.oglowo.callfire.{BetterEnumeration, BetterEnumerationCompanion}

sealed trait Result extends BetterEnumeration
object Result extends BetterEnumerationCompanion[Result] {
  override val values: Set[Result] = Set(
    LiveAnswerResult,
    AnsweringMachineResult,
    BusyResult,
    DoNotCallResult,
    TransferResult,
    TransferLegResult,
    NoAnswerResult,
    UndialedResult,
    SentResult,
    ReceivedResult,
    DoNotTextResult,
    TooBigResult,
    InternalErrorResult,
    CarrierErrorResult,
    CarrierTemporaryError
  )
}

case object LiveAnswerResult extends Result { override def name = "LA" }
case object AnsweringMachineResult extends Result { override def name = "AM" }
case object BusyResult extends Result { override def name = "BUSY" }
case object DoNotCallResult extends Result { override def name = "DNC" }
case object TransferResult extends Result { override def name = "XFER" }
case object TransferLegResult extends Result { override def name = "XFER_LEG" }
case object NoAnswerResult extends Result { override def name = "NO_ANS" }
case object UndialedResult extends Result { override def name = "UNDIALED" }
case object SentResult extends Result { override def name = "SENT" }
case object ReceivedResult extends Result { override def name = "RECEIVED" }
case object DoNotTextResult extends Result { override def name = "DNT" }
case object TooBigResult extends Result { override def name = "TOO_BIG" }
case object InternalErrorResult extends Result { override def name = "INTERNAL_ERROR" }
case object CarrierErrorResult extends Result { override def name = "CARRIER_ERROR" }
case object CarrierTemporaryError extends Result { override def name = "CARRIER_TEMP_ERROR" }

