package com.oglowo.callfire.callfirexml

import scala.xml.{TopScope, UnprefixedAttribute, MetaData, Elem}

object Util {
  def metadata(attributes: Map[String, _]): MetaData = {
    // do some recursive shit here... might not be in this foreach, but we need to convert this set of attributes
    // into some attr(attr(attr(attr())) type structure
    null
  }
}

case class DialPlan(name: Option[String] = None, loggingEnabled: Boolean = true, body: Seq[CallFireXmlTag])
  extends Elem(null,
    "dialplan",
    Util.metadata(Map(
        "name" -> name,
        "loggingEnabled" -> loggingEnabled)),
    TopScope,
    false,
    body:_*) with CallFireXmlTag {

}
