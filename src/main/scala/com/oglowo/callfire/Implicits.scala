package com.oglowo.callfire

object Implicits extends Implicits
trait Implicits extends ClassDefinitions {
  implicit def StringWithPrecondition2String(string: StringWithPrecondition) = string.underlying
  implicit def IntWithPrecondition2Int(value: IntWithPrecondition) = value.underlying

  implicit def Int2OneDigitInt(value: Int) = OneDigitInt(value)
  implicit def Int2TwoDigitInt(value: Int) = TwoDigitInt(value)
  implicit def Int2ThreeDigitInt(value: Int) = ThreeDigitInt(value)
  implicit def Int2FourDigitInt(value: Int) = FourDigitInt(value)
  implicit def Int2FiveDigitInt(value: Int) = FiveDigitInt(value)

  implicit def Int2Min1DigitInt(value: Int) = Min1DigitInt(value)
  implicit def Int2Min2DigitInt(value: Int) = Min2DigitInt(value)
  implicit def Int2Min3DigitInt(value: Int) = Min3DigitInt(value)
  implicit def Int2Min4DigitInt(value: Int) = Min4DigitInt(value)
  implicit def Int2Min5DigitInt(value: Int) = Min5DigitInt(value)

  implicit def String2String1(string: String) = String1(string)
  implicit def String2String2(string: String) = String2(string)
  implicit def String2String3(string: String) = String3(string)
  implicit def String2String4(string: String) = String4(string)
  implicit def String2String5(string: String) = String5(string)

  implicit def String2MaxString1(string: String) = MaxString1(string)
  implicit def String2MaxString2(string: String) = MaxString2(string)
  implicit def String2MaxString3(string: String) = MaxString3(string)
  implicit def String2MaxString4(string: String) = MaxString4(string)
  implicit def String2MaxString5(string: String) = MaxString5(string)
  implicit def String2MaxString15(string: String) = MaxString15(string)
}
