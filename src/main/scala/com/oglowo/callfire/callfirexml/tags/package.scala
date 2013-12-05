package com.oglowo.callfire.callfirexml

import scala.xml._
import spray.http.Uri
import com.oglowo.callfire.entity.PhoneNumber

package object tags {
  abstract class CallFireXmlTag(val name: Option[String],
                                val tagPrefix: Option[String] = None,
                                override val label: String,
                                val tagAttributes: Map[String, _],
                                override val scope: NamespaceBinding = TopScope,
                                override val minimizeEmpty: Boolean = false,
                                val body: Seq[Node] = Seq.empty) extends Elem(tagPrefix.orNull, label, metadata(tagAttributes), scope, minimizeEmpty, body:_*)

  abstract class CallFireTextTag[T](content: T) extends Text(content.toString)

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
  case object UrlPlayType extends PlayType { val value = "url" }
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

  case class UriNode(uri: Uri) extends CallFireTextTag[Uri](uri)
  case class TtsNode(content: String) extends CallFireTextTag[String](content)
  case class SoundIdNode(id: Long) extends CallFireTextTag[Long](id)

  case class Play(override val name: Option[String] = None, playType: PlayType, voice: Option[Voice] = None, cache: Boolean, content: CallFireTextTag[_])
    extends CallFireXmlTag(name = name,
      label = "play",
      tagAttributes = Map("name" -> name, "type" -> playType.value, "voice" -> voice.map(_.value), "cache" -> cache),
      body = Seq(content))

  class KeyPressKey(val value: String) {
    //require(KeyPressKey.ValidKeys.contains(value))
  }
  object KeyPressKey {
    val ValidKeys = Set("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "#", "Default", "Timeout")
  }

  case object `0` extends KeyPressKey("0")
  case object `1` extends KeyPressKey("1")
  case object `2` extends KeyPressKey("2")
  case object `3` extends KeyPressKey("3")
  case object `4` extends KeyPressKey("4")
  case object `5` extends KeyPressKey("5")
  case object `6` extends KeyPressKey("6")
  case object `7` extends KeyPressKey("7")
  case object `8` extends KeyPressKey("8")
  case object `9` extends KeyPressKey("9")
  case object `*` extends KeyPressKey("*")
  case object `#` extends KeyPressKey("#")
  case object `Default` extends KeyPressKey("Default")
  case object `Timeout` extends KeyPressKey("Timeout")

  case class KeyPress(override val name: Option[String] = None, pressed: KeyPressKey, override val body: Seq[CallFireXmlTag])
    extends CallFireXmlTag(name = name,
      label = "keypress",
      tagAttributes = Map("pressed" -> pressed.value),
      body = body)

  case class SetVar(override val name: Option[String] = None, variableName: String, variableValue: Expression)
    extends CallFireXmlTag(name = name,
      label = "setvar",
      tagAttributes = Map("name" -> name, "varname" -> variableName),
      body = Text(variableValue.value))

  // locationUri can have CallFireXML expressions in it which is why it's not a Uri type
  case class Get(override val name: Option[String] = None, variableName: String, locationUri: Expression)
    extends CallFireXmlTag(name = name,
      label = "get",
      tagAttributes = Map("name" -> name, "varname" -> variableName),
      body = locationUri)

  case class Post(override val name: Option[String] = None, variableName: String, locationUri: Expression)
    extends CallFireXmlTag(name = name,
      label = "post",
      tagAttributes = Map("name" -> name, "varname" -> variableName),
      body = locationUri)

  case class Record(override val name: Option[String] = None, variableName: String, background: Boolean = true, timeoutMilliseconds: Option[Long] = None)
    extends CallFireXmlTag(name = name,
      label = "record",
      tagAttributes = Map("name" -> name, "varname" -> variableName, "background" -> background, "timeout" -> timeoutMilliseconds))

  class MusicOnHold(val value: String) {
    require(MusicOnHold.ValidValues.contains(value))
  }
  object MusicOnHold {
    val ValidValues = Set("silence", "default")
  }

  case object Silence extends MusicOnHold("silence")
  case object DefaultMusic extends MusicOnHold("default")

  class RingMode(val value: String) {
    require(RingMode.ValidValues.contains(value))
  }
  object RingMode {
    val ValidValues = Set("waterfall", "ringall")
  }

  case object WaterfallRingMode extends RingMode("waterfall")
  case object RingAllRingMode extends RingMode("ringall")

  case class Expression(value: String) extends CallFireTextTag[String](value)

  case class Transfer(override val name: Option[String] = None, callerId: Either[PhoneNumber, Expression], callerIdAlpha: Option[String] = None, musicOnHold: MusicOnHold = DefaultMusic, continueAfter: Boolean = false, ringMode: RingMode = RingAllRingMode, timeoutSeconds: Option[Int] = None, whisperTextToSpeech: Option[String] = None, numbers: Seq[PhoneNumber] = Seq.empty, screen: Boolean = false)
    extends CallFireXmlTag(name = name,
      label = "transfer",
      tagAttributes = Map(
        "name" -> name,
        "callerid" -> { callerId match {
          case Left(number) => number.number.toString
          case Right(expression) => expression.value
        }},
        "calleridalpha" -> callerIdAlpha,
        "musiconhold" -> musicOnHold.value,
        "continue-after" -> continueAfter,
        "mode" -> ringMode.value,
        "timeout" -> timeoutSeconds,
        "whisper-tts" -> whisperTextToSpeech,
        "screen" -> screen
      ),
      body = Seq(Text(numbers.map(_.number.toString).mkString(", ")))) {
    if (screen == true) require(whisperTextToSpeech.isDefined && !whisperTextToSpeech.get.trim.isEmpty, "If you want to screen calls, you must set the whisper tts. For example, Call from 2 1 3 4 4 8 5 9 1 6. Press 1 to accept.")
  }

  case class Goto(override val name: Option[String] = None, nodeName: String)
    extends CallFireXmlTag(name = name,
      label = "goto",
      tagAttributes = Map("name" -> name),
      body = Seq(Text(nodeName)))

  case class If(override val name: Option[String] = None, expression: Expression, override val body: Seq[CallFireXmlTag])
    extends CallFireXmlTag(
      name = name,
      label = "if",
      tagAttributes = Map("expr" -> expression.value),
      body = body
    )

  case class Equal(override val name: Option[String] = None, variable: Expression, expression: Expression, override val body: Seq[CallFireXmlTag])
    extends CallFireXmlTag(
      name = name,
      label = "equal",
      tagAttributes = Map(
        "var" -> variable.value,
        "expr" -> expression.value),
      body = body
    )

  case class NotEqual(override val name: Option[String] = None, variable: Expression, expression: Expression, override val body: Seq[CallFireXmlTag])
    extends CallFireXmlTag(
      name = name,
      label = "notEqual",
      tagAttributes = Map(
        "var" -> variable.value,
        "expr" -> expression.value),
      body = body
    )

  class CatchType(val value: String) {
    require(CatchType.ValidValues.contains(value))
  }
  object CatchType {
    val ValidValues = Set("hangup", "digit")
  }

  case object HangupCatchType extends CatchType("hangup")
  case object DigitCatchType extends CatchType("digit")

  case class Catch(override val name: Option[String] = None, `type`: CatchType, value: Option[String] = None, goto: String, override val body: Seq[CallFireXmlTag])
    extends CallFireXmlTag(
      name = name,
      label = "catch",
      tagAttributes = Map(
        "type" -> `type`.value,
        "value" -> value,
        "goto" -> goto
      ),
      body = body
    )
}
