package com.oglowo.callfire.callfirexml

import scala.xml._
import spray.http.Uri

package object tags {
  abstract class CallFireXmlTag(val name: Option[String],
                                val tagPrefix: Option[String] = None,
                                override val label: String,
                                val tagAttributes: Map[String, _],
                                override val scope: NamespaceBinding = TopScope,
                                override val minimizeEmpty: Boolean = false,
                                val body: Seq[Node] = Seq.empty) extends Elem(tagPrefix.orNull, label, metadata(tagAttributes), scope, minimizeEmpty, body:_*)

  case class DialPlan(override val name: Option[String] = None, loggingEnabled: Boolean = true, override val body: Seq[CallFireXmlTag])
    extends CallFireXmlTag(name = name,
      label = "dialplan",
      tagAttributes = Map("name" -> name, "loggingEnabled" -> loggingEnabled),
      body = body)

  case class GotoXml(override val name: Option[String] = None, uri: Uri)
    extends CallFireXmlTag(name = name,
      label = "gotoXML",
      tagAttributes = Map("name" -> name),
      body = Seq(Text(uri.toString)))

  case class Menu(override val name: Option[String] = None, timeout: Option[Int] = None, maxDigits: Option[Int] = None, override val body: Seq[CallFireXmlTag])
    extends CallFireXmlTag(name = name,
      label = "menu",
      tagAttributes = Map("name" -> name, "timeout" -> timeout, "maxDigits" -> maxDigits),
      body = body)

  sealed trait PlayType {
    val value: String
  }
  object PlayType {
    def values = Seq(CallFireIdPlayType, UrlPlayType, TtsPlayType)
    def withName(name: String) = values.find(_.value == name).get
  }
  case object CallFireIdPlayType extends PlayType { val value = "callfireid" }
  case object UrlPlayType extends PlayType { val value = "URL" }
  case object TtsPlayType extends PlayType { val value = "tts" }

  sealed trait Voice {
    val value: String
  }
  object Voice {
    def values = Seq(MaleOneVoice, FemaleOneVoice, FemaleTwoVoice, FemaleSpanishVoice)
    def withName(name: String) = values.find(_.value == name).get
  }

  case object MaleOneVoice extends Voice { val value = "male1" }
  case object FemaleOneVoice extends Voice { val value = "female1" }
  case object FemaleTwoVoice extends Voice { val value = "female2" }
  case object FemaleSpanishVoice extends Voice { val value = "femaleSpanish" }

//  case class Play(override val name: Option[String] = None, playType: PlayType, voice: Option[Voice] = None, cache: Boolean)
//    extends CallFireXmlTag(name = name,
//      label = "play",
//      tagAttributes = Map("name" -> name, "type" -> playType, "voice" -> voice, "cache" -> cache),
//      )
}
