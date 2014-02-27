package com.oglowo.callfire

import com.typesafe.scalalogging.log4j.Logging
import spray.httpx.UnsuccessfulResponseException
import scala.util.{Failure, Random, Success}
import com.oglowo.callfire.entity._
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import com.oglowo.callfire.Imports._
import com.oglowo.callfire.pimps._
import scalaz._
import Scalaz._
import scala.util.Success
import scala.util.Failure
import scala.Some
import java.io.{File, FileOutputStream}
import java.nio.ByteBuffer
import org.joda.money.{CurrencyUnit, Money}
import akka.dispatch.Futures
import java.nio.charset.Charset
import akka.actor.Cancellable

object Main extends Logging {
  val client = new Client with ProductionRunscopeClientConnection

  import client._

  def printError(error: Throwable) = {
    error match {
      case e: UnsuccessfulResponseException => println(s"!!>> API ERROR ${e.asApiError}")
      case e: Throwable =>
        println(s"!!!>>>!!! NON API ERROR")
        e.printStackTrace()

    }
  }

  def runTryPurchaseAndConfigureNumber(client: Client, prefix: Int, city: String, count: Int) = {
    val dialplan = <dialplan name="Root">
      <menu name="main_menu" maxDigits="1" timeout="3500">
        <play type="tts" voice="female2">1 2 3 and to the 4. snoop doggy dog and doctor dre is at the door</play>
        <keypress pressed="2">
          <transfer name="transfer_adrian" callerid="${call.callerid}" mode="ringall" screen="true" whisper-tts="yyyyYo yo yo press 1 if you want to take this here call, son!">
            12132228559,13107738288
          </transfer>
        </keypress>
        <keypress pressed="1">
          <play name="ethnic_woman_talking_shit" type="tts" voice="spanish1">Hijo de to pinchi madre. Vete a la puta verga, pendejo!</play>
        </keypress>
      </menu>
    </dialplan>

    println(s"Searching for $count max numbers with prefix $prefix in $city")
    //client.searchForNumbers(implicitly[Min4DigitInt](prefix).some, city.some, count) onComplete {
    client.searchForTollFreeNumbers(implicitly[Min4DigitInt](prefix).some, count) onComplete {
      case Success(response) => {
        if (!response.isEmpty) {
          println("[[[ NUMBERS FOUND ]]]")
          response.foreach(number => println(number.nationalFormat))
          val phoneNumberLocationToPurchase = new Random(System.currentTimeMillis()).nextInt(response.length)
          val number = response(phoneNumberLocationToPurchase)
          println(s"!!!!![[[[[[ I'm going to try and purchase ${number.nationalFormat}")
          client.orderNumbers(Set(number)) onComplete {
            case Success(orderReference) => {
              println("....checking order status....")
              client.getOrder(orderReference) onComplete {
                case Success(order) => {
                  println("Order is in " + order.status.name + " state")
                  var done = order.status match {
                    case FinishedOrderStatus => true
                    case ErroredOrderStatus => true
                    case VoidOrderStatus => true
                    case _ => false
                  }
                  var theOrder = order
                  while (!done) {
                    Thread.sleep(1000)
                    theOrder = Await.result(client.getOrder(orderReference), Duration.Inf)
                    println(s"[[[....checking order status....]]] ====> ${theOrder.status.name}")
                    done = theOrder.status match {
                      case FinishedOrderStatus => true
                      case ErroredOrderStatus => true
                      case VoidOrderStatus => true
                      case _ => false
                    }
                  }

                  theOrder.tollFreeNumbers match {
                    case Some(orderItem) => {
                      if (!orderItem.itemsFulfilled.isEmpty) {
                        println(".... LOOKS LIKE WE FULFILLED YOUR ORDER ....")
                        val inboundConfiguration = InboundIvrConfiguration(dialplan.some, number.number.some)
                        val configuration = PhoneNumberConfiguration(EnabledPhoneNumberFeature.some, DisabledPhoneNumberFeature.some, inboundConfiguration.some)
                        val theNumber = orderItem.itemsFulfilled(0).copy(configuration = configuration.some)
                        client.configureNumber(theNumber) onComplete {
                          case Success(s) => {
                            println(".... OK try to call your number " + s.nationalFormat + " now")
                            client.shutdown()
                          }
                          case Failure(error) => {
                            printError(error)
                            client.shutdown()
                          }
                        }
                      }
                      else {
                        println("... no numbers fulfilled. ...")
                        client.shutdown()
                      }
                    }
                    case _ => {
                      println("... no numbers fulfilled. ...")
                      client.shutdown()
                    }
                  }
                }
                case Failure(error) => {
                  printError(error)
                  client.shutdown()
                }
              }
            }
            case Failure(error) => {
              printError(error)
              client.shutdown()
            }
          }
        }
        else {
          println("No numbers found for that criteria")
          client.shutdown()
        }
      }
      case Failure(error) => {
        printError(error)
        client.shutdown()
      }
    }
  }

  def runBulkOrderPurchase(purchaseMap: Map[String, Int]) = {
    var numbersOrdered = 0
    var totalCost: Money = Money.of(CurrencyUnit.USD, 0)
    val map = purchaseMap.map(entry => (implicitly[Min4DigitInt](("1" + entry._1).toInt), entry._2))
    map.map(entry => {
      val bulkOrderFuture = client.bulkOrderNumbers(entry._1.some, None, entry._2)
      bulkOrderFuture onComplete {
        case Success(orderReference) => {
          val iterator = Iterator.continually({
            Thread.sleep(1000); Await.result(client.getOrder(orderReference), Duration.Inf)
          }).takeWhile(order => {
            if (order.status != ProcessingOrderStatus) {
              order.status match {
                case FinishedOrderStatus => {
                  order.localNumbers match {
                    case Some(numbers) => {
                      numbersOrdered += 1
                      totalCost = totalCost.plus(order.totalCost)
                      if (numbers.itemsFulfilled.size == entry._2) println("The order " + entry + " was finished SUCCESSFULLY for a total cost of " + order.totalCost.toString)
                      else println("The order " + entry + " was finished, but with " + numbers.itemsFulfilled.length + "numbers instead of " + entry._2 + " for a total cost of " + order.totalCost.toString)
                    }
                    case None => println("The order " + entry + " was finished, but did not fulfill any numbers :( " + order)
                  }
                }
                case default => {
                  println("The order " + entry + " was finished. It ended up in " + default.name + " status. Here's the order: " + order)
                }
              }
            }
            order.status == ProcessingOrderStatus
          })
          iterator.foreach(order => println("<<< Processing order for " + entry + " >>>"))
          while (iterator.hasNext) {

          }
          client.shutdown()

          println("<<<<<<<<<< ORDER COMPLETE >>>>>>>>>>\n")
          println(s"Total Numbers: $numbersOrdered")
          println(s"Total Cost:    ${totalCost.toString}\n")
          println("------------------------------------")
        }
        case Failure(error) => {
          error match {
            case e: UnsuccessfulResponseException => println("The order " + entry + " could not be completed because " + e.asApiError.message)
            case e: Throwable => println("The order " + entry + " could not be completed because of a system error \"" + e.getMessage + "\"")
          }
        }
      }
      bulkOrderFuture
    }).toList
  }

  def main(args: Array[String]) {

    //    import com.oglowo.callfire.callfirexml.tags._
    //    DialPlan(body = Seq(Play(playType = UrlPlayType, voice = FemaleOneVoice.some, cache = false, body = Seq(
    //      Menu(body = Seq(KeyPress(pressed = `1`, body = Seq(Transfer(callerId = PhoneNumber("12134485916").left, )))))
    //    ))))
    //    val prefix = args(0)
    //    val city = args(1)
    //    val count = args(2)
    //
    //    runTryPurchaseAndConfigureNumber(client, prefix.toInt, city, count.toInt)
    val dialplan =
      <dialplan loggingEnabled="true">
        <post varname="new_call_event_post_response">
          https://api.vonjour.com/webhooks/callfire/events?type=NewCallToOrganization
          &amp;
          callId=${{call.id}}
          &amp;
          entityId=c531c870-5c75-11e3-a053-90b8d02a6319
        </post>
        <menu>
          <play cache="false" type="tts">
            Hi fella. Press 0
          </play>
          <keypress pressed="321">
            <setvar varname="voicemail_recipient_type">users</setvar>
            <setvar varname="recipient_id">c4a1bd70-5c75-11e3-927e-90b8d0128a60</setvar>
            <setvar varname="voicemail_greeting">
              https://us-east.manta.joyent.com/vonjourvoix/public/audio/greetings/users/c4a1bd70-5c75-11e3-927e-90b8d0128a60/user-voicemail-greeting/31e88e10-9383-11e3-87d1-90b8d02a6319.16bit8khzmono.wav
            </setvar>
            <post varname="call_transferring_to_user_event_post_response">
              https://api.vonjour.com/webhooks/callfire/events?type=CallTransferringToUser
              &amp;
              callId=${{call.id}}
              &amp;
              entityId=c4a1bd70-5c75-11e3-927e-90b8d0128a60
            </post>
            <play cache="false" type="url">
              https://us-east.manta.joyent.com/vonjourvoix/public/default-call-forwarding-greeting-16bit8khzmono.wav
            </play>
            <transfer musiconhold="default" timeout="20" mode="waterfall" screen="true" callerid="${{call.dnis}}"
                      whisper-tts="Hello. Incoming vonjour call. Press 1 to accept" continue-after="false">19514030229
            </transfer>
          </keypress>
          <keypress pressed="4">
            <setvar varname="voicemail_recipient_type">groups</setvar>
            <setvar varname="recipient_id">a4f4d1b0-8e07-11e3-b934-90b8d02a6319</setvar>
            <setvar varname="voicemail_greeting">
              https://us-east.manta.joyent.com/vonjourvoix/public/default-voicemail-greeting-16bit8khzmono.wav
            </setvar>
            <post varname="call_transferring_to_group_event_post_response">
              https://api.vonjour.com/webhooks/callfire/events?type=CallTransferringToGroup
              &amp;
              callId=${{call.id}}
              &amp;
              entityId=a4f4d1b0-8e07-11e3-b934-90b8d02a6319
            </post>
            <play cache="false" type="url">
              https://us-east.manta.joyent.com/vonjourvoix/public/default-call-forwarding-greeting-16bit8khzmono.wav
            </play>
            <transfer musiconhold="default" timeout="20" mode="waterfall" screen="true" callerid="${{call.dnis}}"
                      whisper-tts="Hello. Incoming vonjour Investor relations call. Press 1 to accept."
                      continue-after="false" name="transfer_a4f4d1b0_8e07_11e3_b934_90b8d02a6319">19514030229,
              12134485916, 13105551233
            </transfer>
          </keypress>
          <keypress pressed="703">
            <setvar varname="voicemail_recipient_type">users</setvar>
            <setvar varname="recipient_id">50971240-5d33-11e3-a6f1-90b8d02a6319</setvar>
            <setvar varname="voicemail_greeting">
              https://us-east.manta.joyent.com/vonjourvoix/public/default-voicemail-greeting-16bit8khzmono.wav
            </setvar>
            <post varname="call_transferring_to_user_event_post_response">
              https://api.vonjour.com/webhooks/callfire/events?type=CallTransferringToUser
              &amp;
              callId=${{call.id}}
              &amp;
              entityId=50971240-5d33-11e3-a6f1-90b8d02a6319
            </post>
          </keypress>
          <keypress pressed="3">
            <setvar varname="voicemail_recipient_type">groups</setvar>
            <setvar varname="recipient_id">b1137420-6863-11e3-82f7-90b8d0128a60</setvar>
            <setvar varname="voicemail_greeting">
              https://us-east.manta.joyent.com/vonjourvoix/public/default-voicemail-greeting-16bit8khzmono.wav
            </setvar>
            <post varname="call_transferring_to_group_event_post_response">
              https://api.vonjour.com/webhooks/callfire/events?type=CallTransferringToGroup
              &amp;
              callId=${{call.id}}
              &amp;
              entityId=b1137420-6863-11e3-82f7-90b8d0128a60
            </post>
            <play cache="false" type="url">
              https://us-east.manta.joyent.com/vonjourvoix/public/default-call-forwarding-greeting-16bit8khzmono.wav
            </play>
            <transfer musiconhold="default" timeout="20" mode="waterfall" screen="true" callerid="${{call.dnis}}"
                      whisper-tts="Hello. Incoming vonjour Support call. Press 1 to accept." continue-after="false"
                      name="transfer_b1137420_6863_11e3_82f7_90b8d0128a60">12134485916, 13105551233, 13109755640
            </transfer>
          </keypress>
          <keypress pressed="213">
            <setvar varname="voicemail_recipient_type">users</setvar>
            <setvar varname="recipient_id">dcb4ef00-5d32-11e3-b31f-90b8d0128a60</setvar>
            <setvar varname="voicemail_greeting">
              https://us-east.manta.joyent.com/vonjourvoix/public/default-voicemail-greeting-16bit8khzmono.wav
            </setvar>
            <post varname="call_transferring_to_user_event_post_response">
              https://api.vonjour.com/webhooks/callfire/events?type=CallTransferringToUser
              &amp;
              callId=${{call.id}}
              &amp;
              entityId=dcb4ef00-5d32-11e3-b31f-90b8d0128a60
            </post>
            <play cache="false" type="url">
              https://us-east.manta.joyent.com/vonjourvoix/public/default-call-forwarding-greeting-16bit8khzmono.wav
            </play>
            <transfer musiconhold="default" timeout="20" mode="waterfall" screen="true" callerid="${{call.dnis}}"
                      whisper-tts="Hello. Incoming vonjour call. Press 1 to accept" continue-after="true">12134485916
            </transfer>
          </keypress>
          <keypress pressed="1">
            <setvar varname="voicemail_recipient_type">groups</setvar>
            <setvar varname="recipient_id">9aedbed0-6863-11e3-82f7-90b8d0128a60</setvar>
            <setvar varname="voicemail_greeting">
              https://us-east.manta.joyent.com/vonjourvoix/public/default-voicemail-greeting-16bit8khzmono.wav
            </setvar>
            <post varname="call_transferring_to_group_event_post_response">
              https://api.vonjour.com/webhooks/callfire/events?type=CallTransferringToGroup
              &amp;
              callId=${{call.id}}
              &amp;
              entityId=9aedbed0-6863-11e3-82f7-90b8d0128a60
            </post>
            <play cache="false" type="url">
              https://us-east.manta.joyent.com/vonjourvoix/public/default-call-forwarding-greeting-16bit8khzmono.wav
            </play>
            <transfer musiconhold="default" timeout="20" mode="waterfall" screen="true" callerid="${{call.dnis}}"
                      whisper-tts="Hello. Incoming vonjour Billing call. Press 1 to accept." continue-after="true"
                      name="transfer_9aedbed0_6863_11e3_82f7_90b8d0128a60">19514030229, 12134485916, 13105551233
            </transfer>
          </keypress>
          <keypress pressed="701">
            <setvar varname="voicemail_recipient_type">users</setvar>
            <setvar varname="recipient_id">e7bfe5f0-95b1-11e3-af41-90b8d0128a60</setvar>
            <setvar varname="voicemail_greeting">
              https://us-east.manta.joyent.com/vonjourvoix/public/default-voicemail-greeting-16bit8khzmono.wav
            </setvar>
            <post varname="call_transferring_to_user_event_post_response">
              https://api.vonjour.com/webhooks/callfire/events?type=CallTransferringToUser
              &amp;
              callId=${{call.id}}
              &amp;
              entityId=e7bfe5f0-95b1-11e3-af41-90b8d0128a60
            </post>
            <play cache="false" type="url">
              https://us-east.manta.joyent.com/vonjourvoix/public/default-call-forwarding-greeting-16bit8khzmono.wav
            </play>
            <transfer musiconhold="default" timeout="20" mode="waterfall" screen="false" callerid="${{call.dnis}}"
                      whisper-tts="Incoming vonjour call." continue-after="false">13105551233
            </transfer>
          </keypress>
          <keypress pressed="702">
            <setvar varname="voicemail_recipient_type">users</setvar>
            <setvar varname="recipient_id">742fab30-96cd-11e3-82a2-90b8d0128a60</setvar>
            <setvar varname="voicemail_greeting">
              https://us-east.manta.joyent.com/vonjourvoix/public/default-voicemail-greeting-16bit8khzmono.wav
            </setvar>
            <post varname="call_transferring_to_user_event_post_response">
              https://api.vonjour.com/webhooks/callfire/events?type=CallTransferringToUser
              &amp;
              callId=${{call.id}}
              &amp;
              entityId=742fab30-96cd-11e3-82a2-90b8d0128a60
            </post>
            <play cache="false" type="url">
              https://us-east.manta.joyent.com/vonjourvoix/public/default-call-forwarding-greeting-16bit8khzmono.wav
            </play>
            <transfer musiconhold="default" timeout="20" mode="waterfall" screen="false" callerid="${{call.dnis}}"
                      whisper-tts="Incoming vonjour call." continue-after="false">13109755640
            </transfer>
          </keypress>
          <keypress pressed="10">
            <setvar varname="voicemail_recipient_type">groups</setvar>
            <setvar varname="recipient_id">109f3b90-98cf-11e3-b355-90b8d0128a60</setvar>
            <setvar varname="voicemail_greeting">
              https://us-east.manta.joyent.com/vonjourvoix/public/default-voicemail-greeting-16bit8khzmono.wav
            </setvar>
            <post varname="call_transferring_to_group_event_post_response">
              https://api.vonjour.com/webhooks/callfire/events?type=CallTransferringToGroup
              &amp;
              callId=${{call.id}}
              &amp;
              entityId=109f3b90-98cf-11e3-b355-90b8d0128a60
            </post>
            <play cache="false" type="url">
              https://us-east.manta.joyent.com/vonjourvoix/public/default-call-forwarding-greeting-16bit8khzmono.wav
            </play>
            <transfer musiconhold="default" timeout="20" mode="waterfall" screen="true" callerid="${{call.dnis}}"
                      whisper-tts="Hello. Incoming vonjour Analytics call. Press 1 to accept." continue-after="false"
                      name="transfer_109f3b90_98cf_11e3_b355_90b8d0128a60">19514030229, 12134485916, 13105551233
            </transfer>
          </keypress>
          <keypress pressed="0">
            <setvar varname="voicemail_recipient_type">groups</setvar>
            <setvar varname="recipient_id">c68bda30-5c75-11e3-927e-90b8d0128a60</setvar>
            <setvar varname="voicemail_greeting">
              https://us-east.manta.joyent.com/vonjourvoix/public/default-voicemail-greeting-16bit8khzmono.wav
            </setvar>
            <play cache="false" type="tts">
              hold up
            </play>
            <transfer musiconhold="default" timeout="40" mode="waterfall" screen="true" callerid="${{call.dnis}}"
                      whisper-tts="Hello. Incoming vonjour Operator call. Press 1 to accept." continue-after="true"
                      name="transfer_c68bda30_5c75_11e3_927e_90b8d0128a60">19514030229</transfer>
          </keypress>
          <keypress pressed="2">
            <setvar varname="voicemail_recipient_type">groups</setvar>
            <setvar varname="recipient_id">a5c2e0b0-6863-11e3-82f7-90b8d0128a60</setvar>
            <setvar varname="voicemail_greeting">
              https://us-east.manta.joyent.com/vonjourvoix/public/default-voicemail-greeting-16bit8khzmono.wav
            </setvar>
            <post varname="call_transferring_to_group_event_post_response">
              https://api.vonjour.com/webhooks/callfire/events?type=CallTransferringToGroup
              &amp;
              callId=${{call.id}}
              &amp;
              entityId=a5c2e0b0-6863-11e3-82f7-90b8d0128a60
            </post>
            <play cache="false" type="url">
              https://us-east.manta.joyent.com/vonjourvoix/public/default-call-forwarding-greeting-16bit8khzmono.wav
            </play>
            <transfer musiconhold="default" timeout="20" mode="waterfall" screen="true" callerid="${{call.dnis}}"
                      whisper-tts="Hello. Incoming vonjour Accounting call. Press 1 to accept." continue-after="false"
                      name="transfer_a5c2e0b0_6863_11e3_82f7_90b8d0128a60">19514030229
            </transfer>
          </keypress>
        </menu>
        <catch goto="voicemail_poster" type="hangup">
          <post varname="send_last_transfer_response">https://b5mwzph4hatx.runscope.net?transferNumber=${{call.lastTransfer}}&amp;callId=${{call.id}}</post>
          <menu>
            <setvar varname="voicemail_post_sent">false</setvar>
            <play cache="false" type="url">${{voicemail_greeting}}</play>
            <record background="false" varname="voicemail_recording" name="recording"></record>
            <play cache="false" type="url" name="voicemail_options">
              https://us-east.manta.joyent.com/vonjourvoix/public/default-voicemail-options-16bit8khzmono.wav
            </play>
            <keypress pressed="1">
              <play cache="false" type="url">${{voicemail_recording}}</play>
              <goto>voicemail_options</goto>
            </keypress>
            <keypress pressed="2">
              <play cache="false" type="tts">Sending your voicemail</play>
              <post varname="voicemail_post_response_internal">
                https://api.vonjour.com/${{voicemail_recipient_type}}/${{recipient_id}}/inbox/messages?voicemailRecordingUri=${{voicemail_recording}}
              </post>
              <setvar varname="voicemail_post_sent">true</setvar>
              <play cache="false" type="tts">Your voicemail has been sent</play>
            </keypress>
            <keypress pressed="3">
              <goto>recording</goto>
            </keypress>
          </menu>
        </catch>
        <equal name="voicemail_poster" expr="false" var="${{voicemail_post_sent}}">
          <post varname="voicemail_post_response">
            https://api.vonjour.com/${{voicemail_recipient_type}}/${{recipient_id}}/inbox/messages?voicemailRecordingUri=${{voicemail_recording}}
          </post>
        </equal>
      </dialplan>

    val phoneNumber = PhoneNumber(args(0))
    val inboundConfiguration = InboundIvrConfiguration(dialplan.some, phoneNumber.number.some)
    val configuration = PhoneNumberConfiguration(EnabledPhoneNumberFeature.some, DisabledPhoneNumberFeature.some, inboundConfiguration.some)

    val modifiedPhoneNumber = phoneNumber.copy(configuration = configuration.some)
    logger.info("Configuring " + modifiedPhoneNumber.nationalFormat)
    client.configureNumber(modifiedPhoneNumber) onComplete {
      case Success(s) => logger.info("Successfully configured your number!")
      case Failure(error) => logger.error("Error Configuring your number", error)
    }
  }
}

//    val callId = args(0).toLong                                                         `
//    val callFuture = client.getCall(callId)
//
//    callFuture.flatMap(call => {
//      client.getVoicemailSoundFromCall(call)
//    }) onComplete {
//      case Success(s) => {
//        client.shutdown()
//        println("Here is the data: " + s)
//        s.foreach(print)
//      }
//      case Failure(error) => {
//        client.shutdown()
//        println("Error trying to get voicemailsound for call: " + error)
//      }
//    }
//    val phoneNumber = PhoneNumber(args(0))
//    val inboundConfiguration = InboundIvrConfiguration(dialplan.some, phoneNumber.number.some)
//    val configuration = PhoneNumberConfiguration(EnabledPhoneNumberFeature.some, DisabledPhoneNumberFeature.some, inboundConfiguration.some)
//
//    val modifiedPhoneNumber = phoneNumber.copy(configuration = configuration.some)
//    //client.configureNumber(modifiedPhoneNumber) onComplete {
//    client.recordSoundViaPhone(PhoneNumber("2134485916")) onComplete {
//      case Success(number) => {
//        println("The newly configured number is " + number)
//        client.shutdown()
//      }
//      case Failure(error) => {
//        printError(error)
//        client.shutdown()
//      }
//    }
//    val callId = args(0).toLong
//    client.getCall(callId) onComplete {
//      case Success(s) => {
//        client.shutdown()
//        println("BONERS: " + s)
//      }
//      case Failure(error) => {
//        printError(error)
//        client.shutdown()
//      }
//    }

//    client.getNumber(PhoneNumber(number)) onComplete {
//      case Success(phoneNumber) => {
//        println(phoneNumber)
//        client.shutdown()
//      }
//      case Failure(error) => {
//        printError(error)
//        client.shutdown()
//      }
//    }

//runBulkOrderPurchase(purchaseMap)


//    client.recordSoundViaPhone(PhoneNumber(12134485916L), "Console Test".some) onComplete {
//      case Success(reference) => {
//        println(s"The Sound Reference is $reference")
//        println("Waiting for you to finish recording...")
//
////        Iterator.continually({Thread.sleep(1000);Await.result(client.getSoundMetaData(reference), 30.seconds)}).takeWhile(soundMetaData => {
////          if (soundMetaData.status != PendingSoundStatus) {
////            println("-----metadata-----")
////            println(soundMetaData)
////            println("------------------")
////          }
////          soundMetaData.status == PendingSoundStatus
////        }).foreach(soundMetaData => {
////          println(s"[[[ SOUND STATUS is ${soundMetaData.status}-${System.currentTimeMillis()}")
////        })
//
//        Iterator.continually(Await.result(client.getSoundMetaData(reference), 30.seconds)).takeWhile(_.status == PendingSoundStatus)
//
//        println(s"OK COOL you finished recording so you can access your sound at ${reference.location}")
//        client.getSound(reference, WavSoundType) onComplete {
//          case Success(soundBytes) => {
//            val temporaryFile = File.createTempFile(s"${reference.id}-${System.currentTimeMillis()}", ".wav")
//            val outputStream = new FileOutputStream(temporaryFile)
//            val bytesWritten = outputStream.getChannel.write(ByteBuffer.wrap(soundBytes))
//            println(s"Wrote $bytesWritten/${soundBytes.length} to $temporaryFile")
//            client.shutdown()
//          }
//          case Failure(error) => {
//            printError(error)
//            client.shutdown()
//          }
//        }
//      }
//      case Failure(error) => {
//        printError(error)
//        client.shutdown()
//      }
//    }
//}
//}
