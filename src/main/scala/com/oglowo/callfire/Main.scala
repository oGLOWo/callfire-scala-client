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
          val iterator = Iterator.continually({Thread.sleep(1000);Await.result(client.getOrder(orderReference), Duration.Inf)}).takeWhile(order => {
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
  <dialplan name="Root">
    <menu>
      <play type="url" cache="false">https://us-east.manta.joyent.com/vonjourvoix/public/default-organization-main-greeting-16bit8khzmono.wav</play>
      <keypress pressed="1">
        <transfer musiconhold="default" mode="waterfall" callerid="${call.callerid}" continue-after="true" screen="true" timeout="30" whisper-tts="Call for the Sales Department. Press 1 to accept">12134485916, 12139154569, 19514030229</transfer>
        <menu>
          <play type="url" cache="false">https://us-east.manta.joyent.com/vonjourvoix/public/default-voicemail-greeting-16bit8khzmono.wav</play>
          <record varname="voicemail_recording_sales" name="recording_sales"/>
          <play type="url" cache="false" name="voicemail_options_sales">https://us-east.manta.joyent.com/vonjourvoix/public/default-voicemail-options-16bit8khzmono.wav</play>
          <keypress pressed="1">
            <play type="url" cache="false">${{voicemail_recording}}</play>
            <goto>voicemail_options_sales</goto>
          </keypress>
          <keypress pressed="2">
            <play type="tts">This shit will now send</play>
            <post name="voicemail_poster_sales" varname="turtles_sales">http://19257835.ngrok.com/groups/c023d010-4403-11e3-a4dd-28cfe90524e9/inbox/messages?voicemailRecordingUri=${{voicemail_recording_sales}}</post>
            <play type="tts">Big booty hoes! Here is the response from the server for sales vm send${{turtles_sales}}</play>
          </keypress>
          <keypress pressed="3">
            <goto>recording_sales</goto>
          </keypress>
        </menu>
      </keypress>
      <keypress pressed="2">
        <menu>
          <play type="url" cache="false">https://us-east.manta.joyent.com/vonjourvoix/public/default-voicemail-greeting-16bit8khzmono.wav</play>
          <record varname="voicemail_recording_support" name="recording_support"/>
          <play type="url" cache="false" name="voicemail_options_support">https://us-east.manta.joyent.com/vonjourvoix/public/default-voicemail-options-16bit8khzmono.wav</play>
          <keypress pressed="1">
            <play type="url" cache="false">${{voicemail_recording_support}}</play>
            <goto>voicemail_options_support</goto>
          </keypress>
          <keypress pressed="2">
            <play type="tts">This shit will now send</play>
            <post name="voicemail_poster_support" varname="turtles_support">http://19257835.ngrok.com/groups/c023d010-4403-11e3-a4dd-28cfe90524e9/inbox/messages?voicemailRecordingUri=${{voicemail_recording_support}}</post>
            <play type="tts">Big booty hoes! Here is the response from the server for support vm send ${{turtles_support}}</play>
          </keypress>
          <keypress pressed="3">
            <goto>recording_support</goto>
          </keypress>
        </menu>
      </keypress>
      <keypress pressed="0">
        <menu>
          <play type="url" cache="false">https://us-east.manta.joyent.com/vonjourvoix/public/default-voicemail-greeting-16bit8khzmono.wav</play>
          <record varname="voicemail_recording" name="recording"/>
          <play type="url" cache="false" name="voicemail_options">https://us-east.manta.joyent.com/vonjourvoix/public/default-voicemail-options-16bit8khzmono.wav</play>
          <keypress pressed="1">
            <play type="url" cache="false">${{voicemail_recording}}</play>
          </keypress>
          <keypress pressed="2">
            <play type="tts">This shit will now send</play>
            <post name="voicemail_poster" varname="turtles">http://19257835.ngrok.com/groups/c023d010-4403-11e3-a4dd-28cfe90524e9/inbox/messages?voicemailRecordingUri=${{voicemail_recording}}</post>
            <play type="tts">Big booty hoes! Here is the response from the server for operator vm send ${{turtles}}</play>
          </keypress>
          <keypress pressed="3">
            <goto>recording</goto>
          </keypress>
        </menu>
      </keypress>
    </menu>
  </dialplan>



    val phoneNumber = PhoneNumber(args(0))
    val inboundConfiguration = InboundIvrConfiguration(dialplan.some, phoneNumber.number.some)
    val configuration = PhoneNumberConfiguration(EnabledPhoneNumberFeature.some, DisabledPhoneNumberFeature.some, inboundConfiguration.some)

    val modifiedPhoneNumber = phoneNumber.copy(configuration = configuration.some)
    client.configureNumber(modifiedPhoneNumber) onComplete {
      case Success(number) => {
        println("The newly configured number is " + number)
        client.shutdown()
      }
      case Failure(error) => {
        printError(error)
        client.shutdown()
      }
    }
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
  }
}


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
