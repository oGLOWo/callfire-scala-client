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
    val dialplan = """<dialplan name="Root">
                     |	<menu name="main_menu" maxDigits="1" timeout="3500">
                     |		<play type="tts" voice="female2">Hey! Press 1 if you want me to tell you off. Press 2 if you want me to transfer you to Latte or Daniel</play>
                     |		<keypress pressed="2">
                     |			<transfer name="transfer_adrian" callerid="${call.callerid}" mode="ringall" screen="true" whisper-tts="yyyyYo yo yo press 1 if you want to take this here call, son!">
                     |        12132228559,13107738288
                     |      </transfer>
                     |		</keypress>
                     |		<keypress pressed="1">
                     |			<play name="ethnic_woman_talking_shit" type="tts" voice="spanish1">Hijo de to pinchi madre. Vete a la puta verga, pendejo!</play>
                     |		</keypress>
                     |	</menu>
                     |</dialplan>
                     | """.stripMargin('|')


    println(s"Searching for $count max numbers with prefix $prefix in $city")
    client.searchForNumbers(implicitly[Min4DigitInt](prefix).some, city.some, count) onComplete {
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

                  theOrder.localNumbers match {
                    case Some(orderItem) => {
                      if (!orderItem.itemsFulfilled.isEmpty) {
                        println(".... LOOKS LIKE WE FULFILLED YOUR ORDER ....")
                        println(".... Configuring your number after 10 seconds ...")

                        Thread.sleep(10000)
                        client.multipartPut(s"/api/1.1/rest/number/${number.number}.json", Some(Map(
                          "CallFeature" -> ("text/plain", "ENABLED"),
                          "TextFeature" -> ("text/plain", "DISABLED"),
                          "InboundCallConfigurationType" -> ("text/plain", "IVR"),
                          "IvrInboundConfig[id]" -> ("text/plain", "the-grande-ivr"),
                          "DialplanXml" -> ("application/xml", dialplan.getBytes("UTF-8"))
                        ))) onComplete {
                          case Success(configureResponse) => {
                            println(".....!!!! OK Please call " + number.nationalFormat + " to test it out !!!!......")
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


    val dialplan = """<dialplan name="Root">
                     |	<menu name="main_menu" maxDigits="1" timeout="3500">
                     |		<play type="tts" voice="female2">Hey! Press 1 if you want me to tell you off. Press 2 if you want me to transfer you to Latte or Daniel</play>
                     |		<keypress pressed="2">
                     |			<transfer name="transfer_adrian" callerid="${call.callerid}" mode="ringall" screen="true" whisper-tts="yyyyYo yo yo press 1 if you want to take this here call, son!">
                     |        12132228559,13107738288
                     |      </transfer>
                     |		</keypress>
                     |		<keypress pressed="1">
                     |			<play name="ethnic_woman_talking_shit" type="tts" voice="spanish1">Hijo de to pinchi madre. Vete a la puta verga, pendejo!</play>
                     |		</keypress>
                     |	</menu>
                     |</dialplan>
                     | """.stripMargin('|')

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
    val prefix = args(0)
    val city = args(1)
    val count = args(2)

    runTryPurchaseAndConfigureNumber(client, prefix.toInt, city, count.toInt)

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
  }
}
