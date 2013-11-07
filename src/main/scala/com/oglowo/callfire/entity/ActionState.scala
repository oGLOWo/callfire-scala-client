package com.oglowo.callfire.entity

import com.oglowo.callfire.{BetterEnumeration, BetterEnumerationCompanion }

sealed trait ActionState extends BetterEnumeration
object ActionState extends BetterEnumerationCompanion[ActionState] {
  val values: Set[ActionState] = Set(
    ReadyActionState,
    SelectedActionState,
    FinishedActionState,
    DoNotCallActionState,
    DuplicateActionState,
    InvalidActionState,
    TimeoutActionState
  )
}

case object ReadyActionState extends ActionState { override def name = "READY" }
case object SelectedActionState extends ActionState { override def name = "SELECTED" }
case object FinishedActionState extends ActionState { override def name = "FINISHED" }
case object DoNotCallActionState extends ActionState { override def name = "DNC" }
case object DuplicateActionState extends ActionState { override def name = "DUP" }
case object InvalidActionState extends ActionState { override def name = "INVALID" }
case object TimeoutActionState extends ActionState { override def name = "TIMEOUT" }