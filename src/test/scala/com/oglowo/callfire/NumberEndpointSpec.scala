package com.oglowo.callfire

import org.specs2.mutable._
import spray.http.{HttpBody, StatusCodes, StatusCode, HttpResponse, ContentTypes}
import ContentTypes._

class NumberEndpointSpec extends Specification {
  "Number Endpoint" should {
    "parse a valid number configured with inbound IVR" {

    }
  }

  trait Credentials extends Scope {
    val apiUser = "8eccf6f02069"
    val apiPassword = "1dd1705ba4fb8bb2"
  }

  trait Data extends Scope {
    val RawInboundIvrConfiguredNumberResponseBody =
      """
        {
          "Resource":{
            "Number":{
              "Number":18554459732,
              "NationalFormat":"(855) 445-9732",
              "TollFree":true,
              "Status":"ACTIVE",
              "LeaseInfo":{
                "LeaseBegin":"2013-07-18Z",
                "LeaseEnd":"2013-08-30Z",
                "AutoRenew":true
              },
              "NumberConfiguration":{
                "CallFeature":"ENABLED",
                "TextFeature":"UNSUPPORTED",
                "InboundCallConfigurationType":"IVR",
                "InboundCallConfiguration":{
                  "IvrInboundConfig":{
                    "@id":"1174186001",
                    "DialplanXml":"<dialplan name=\"VonjourDevelopmentTest\">\n  <menu name=\"main_menu\">\n    <play type=\"tts\" voice=\"female2\">\n    Hello there my fine feathered friend from ${call.callerid}. Press 1 if you want me to tell you off. Press 2 if you want me to transfer you to Adrian or Rob\n    <\/play>\n    <keypress pressed=\"2\">\n      <transfer callerid=\"${call.callerid}\" name=\"transfer_daniel_or_rob\" mode=\"ringall\" whisper-tts=\"Damnit, son! Do you know the Muffin Man? He's on the line!\">\n        12134485916,18319173474\n      <\/transfer>\n    <\/keypress>\n    <keypress pressed=\"1\">\n      <play type=\"tts\" name=\"ethnic_woman_talking_shit\" voice=\"spanish1\">\n        Que lo que manin! Dame luz papi que hay matatan? Sigue con tu baina y te voylalalal a caer en la conga co?o!\n      <\/play>\n    <\/keypress>\n  <\/menu>\n<\/dialplan>"
                  }
                }
              }
            }
          }
        }
      """
    val ValidInboundIvrConfiguredNumberResponse = HttpResponse(StatusCodes.OK, HttpBody(`application/json`, RawInboundIvrConfiguredNumberResponseBody.getBytes))

  }
}
