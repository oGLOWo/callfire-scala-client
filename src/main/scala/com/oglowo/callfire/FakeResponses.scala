package com.oglowo.callfire

import scala.util.Random

trait FakeResponses {
  private[this] val random  = new Random(System.currentTimeMillis)

  val InboundIvrConfiguredNumberGet =
    """
      |{
      |  "Resource":{
      |    "Number":{
      |      "Number":18554459732,
      |      "NationalFormat":"(855) 445-9732",
      |      "TollFree":true,
      |      "Status":"ACTIVE",
      |      "LeaseInfo":{
      |        "LeaseBegin":"2013-07-18Z",
      |        "LeaseEnd":"2013-08-30Z",
      |        "AutoRenew":true
      |      },
      |      "NumberConfiguration":{
      |        "CallFeature":"ENABLED",
      |        "TextFeature":"UNSUPPORTED",
      |        "InboundCallConfigurationType":"IVR",
      |        "InboundCallConfiguration":{
      |          "IvrInboundConfig":{
      |            "@id":"1174186001",
      |            "DialplanXml":"<dialplan name=\"VonjourDevelopmentTest\">\n  <menu name=\"main_menu\">\n    <play type=\"tts\" voice=\"female2\">\n    Hello there my fine feathered friend from ${call.callerid}. Press 1 if you want me to tell you off. Press 2 if you want me to transfer you to Adrian or Rob\n    <\/play>\n    <keypress pressed=\"2\">\n      <transfer callerid=\"${call.callerid}\" name=\"transfer_daniel_or_rob\" mode=\"ringall\" whisper-tts=\"Damnit, son! Do you know the Muffin Man? He's on the line!\">\n        12134485916,18319173474\n      <\/transfer>\n    <\/keypress>\n    <keypress pressed=\"1\">\n      <play type=\"tts\" name=\"ethnic_woman_talking_shit\" voice=\"spanish1\">\n        Que lo que manin! Dame luz papi que hay matatan? Sigue con tu baina y te voylalalal a caer en la conga co?o!\n      <\/play>\n    <\/keypress>\n  <\/menu>\n<\/dialplan>"
      |          }
      |        }
      |      }
      |    }
      |  }
      |}
    """.stripMargin

  val InboundCallTrackingConfiguredNumberGet =
    """
      |{
      |  "Resource":{
      |    "Number":{
      |      "Number":12133426826,
      |      "NationalFormat":"(213) 342-6826",
      |      "TollFree":false,
      |      "Region":{
      |        "Prefix":1213342,
      |        "City":"LOS ANGELES",
      |        "State":"CA",
      |        "Zipcode":90079,
      |        "Country":"US",
      |        "Lata":730,
      |        "RateCenter":"LSAN DA 07",
      |        "Latitude":34.0514,
      |        "Longitude":-118.261,
      |        "TimeZone":"America\/Los_Angeles"
      |      },
      |      "Status":"ACTIVE",
      |      "LeaseInfo":{
      |        "LeaseBegin":"2013-07-18Z",
      |        "LeaseEnd":"2013-08-30Z",
      |        "AutoRenew":true
      |      },
      |      "NumberConfiguration":{
      |        "CallFeature":"ENABLED",
      |        "TextFeature":"DISABLED",
      |        "InboundCallConfigurationType":"TRACKING",
      |        "InboundCallConfiguration":{
      |          "CallTrackingConfig":{
      |            "@id":"1187158001",
      |            "TransferNumber":"12139154569 12134485916",
      |            "Screen":true,
      |            "Record":true,
      |            "IntroSoundId":125098001,
      |            "WhisperSoundId":125099001
      |          }
      |        }
      |      }
      |    }
      |  }
      |}
    """.stripMargin

  val AllNumberGets: Seq[String] = Seq(InboundCallTrackingConfiguredNumberGet, InboundIvrConfiguredNumberGet)

  val PhoneNumberNotFoundError =
    """
      |{
      |  "ResourceException":{
      |    "HttpStatus":404,
      |    "Message":"number 213342682 does not exist, or is inactive"
      |  }
      |}
    """.stripMargin

  val UnauthorizedError =
    """
      |{
      |  "ResourceException":{
      |    "HttpStatus":401,
      |    "Message":"UNAUTHORIZED"
      |  }
      |}
    """.stripMargin

  val AllErrors: Seq[String] = Seq(PhoneNumberNotFoundError, UnauthorizedError)

  def randomNumberGet(): String = AllNumberGets(random.nextInt(AllNumberGets.size))
  def randomError(): String = AllErrors(random.nextInt(AllErrors.size))
}
