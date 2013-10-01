package com.oglowo.callfire

object Imports extends Imports
trait Imports extends ClassDefinitions with Implicits

object ClassDefinitions extends ClassDefinitions
trait ClassDefinitions {
  type StringPreconditionPredicate = String => Boolean
  class StringWithPrecondition(val underlying: String, predicate: StringPreconditionPredicate) {
    require(predicate(underlying))
  }

  type IntPreconditionPredicate = Int => Boolean
  class IntWithPrecondition(val underlying: Int, predicate: IntPreconditionPredicate) {
    require(predicate(underlying))
  }

  class NDigitInt(override val underlying: Int, val numberOfDigits: Int) extends IntWithPrecondition(underlying, _.toString.replaceAll("^0*", "").length == numberOfDigits)
  case class OneDigitInt(override val underlying: Int) extends NDigitInt(underlying, 1)
  case class TwoDigitInt(override val underlying: Int) extends NDigitInt(underlying, 2)
  case class ThreeDigitInt(override val underlying: Int) extends NDigitInt(underlying, 3)
  case class FourDigitInt(override val underlying: Int) extends NDigitInt(underlying, 4)
  case class FiveDigitInt(override val underlying: Int) extends NDigitInt(underlying, 5)

  class MinNDigitInt(override val underlying: Int, val minNumberOfDigits: Int) extends IntWithPrecondition(underlying, _.toString.replaceAll("^0*", "").length >= minNumberOfDigits)
  case class Min1DigitInt(override val underlying: Int) extends MinNDigitInt(underlying, 1)
  case class Min2DigitInt(override val underlying: Int) extends MinNDigitInt(underlying, 2)
  case class Min3DigitInt(override val underlying: Int) extends MinNDigitInt(underlying, 3)
  case class Min4DigitInt(override val underlying: Int) extends MinNDigitInt(underlying, 4)
  case class Min5DigitInt(override val underlying: Int) extends MinNDigitInt(underlying, 5)

  class StringN(override val underlying: String, val length: Int) extends StringWithPrecondition(underlying, _.length == length)
  case class String1(override val underlying: String) extends StringN(underlying, 1)
  case class String2(override val underlying: String) extends StringN(underlying, 2)
  case class String3(override val underlying: String) extends StringN(underlying, 3)
  case class String4(override val underlying: String) extends StringN(underlying, 4)
  case class String5(override val underlying: String) extends StringN(underlying, 5)

  class MaxStringN(override val underlying: String, maxCharacters: Int) extends StringWithPrecondition(underlying, s => s.length > 1 && s.length <= maxCharacters)
  case class MaxString1(override val underlying: String) extends MaxStringN(underlying, 1)
  case class MaxString2(override val underlying: String) extends MaxStringN(underlying, 2)
  case class MaxString3(override val underlying: String) extends MaxStringN(underlying, 3)
  case class MaxString4(override val underlying: String) extends MaxStringN(underlying, 4)
  case class MaxString5(override val underlying: String) extends MaxStringN(underlying, 5)
  case class MaxString15(override val underlying: String) extends MaxStringN(underlying, 15)
}