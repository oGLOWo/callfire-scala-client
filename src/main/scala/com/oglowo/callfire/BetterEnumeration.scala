package com.oglowo.callfire

trait BetterEnumeration {
  // Assuming they are using the sealed trait/case object pattern,
  // the case object will have a class name CaseObjectName$
  def name: String = getClass.getSimpleName.stripSuffix("$")
  def validNames: Seq[String] = Seq(name)
}

trait BetterEnumerationCompanion[T <: BetterEnumeration] {
  val values: Set[T]
  def withName(name: String): T = values.find(_.validNames.contains(name)) match {
    case Some(value) => value
    case None => throw new IllegalArgumentException(s"No enum value named '$name' was found in ${getClass.getName.stripSuffix("$")}")
  }
}
