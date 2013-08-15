package com.oglowo.callfire.callfirexml

import scala.xml._
import spray.http.Uri

package object tags {
  abstract class CallFireXmlTag(name: Option[String],
                                prefix: Option[String] = None,
                                label: String,
                                attributes: Map[String, _],
                                scope: NamespaceBinding = TopScope,
                                minimizeEmpty: Boolean = false,
                                body: Seq[CallFireXmlTag] = Seq.empty) extends Elem(prefix.orNull, label, metadata(attributes), scope, minimizeEmpty, body:_*) {
  }

  case class DialPlan(name: Option[String] = None, loggingEnabled: Boolean = true, body: Seq[CallFireXmlTag])
    extends CallFireXmlTag(name = name,
      label = "dialplan",
      attributes = Map("name" -> name, "loggingEnabled" -> loggingEnabled),
      body = body:_*)

  case class GotoXml(name: Option[String], uri: Uri)
    extends CallFireXmlTag(name = name,
      label = "gotoXML",
      attributes = Map("name" -> name))
}
