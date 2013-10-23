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

                  theOrder.tollFreeNumbers match {
                    case Some(orderItem) => {
                      if (!orderItem.itemsFulfilled.isEmpty) {
                        println(".... LOOKS LIKE WE FULFILLED YOUR ORDER ....")
                        println(".... Configuring your number ...")

                        client.put(s"/api/1.1/rest/number/$number.json", Some(Map(
                          "CallFeature" -> "ENABLED",
                          "TextFeature" -> "DISABLED",
                          "InboundCallConfigurationType" -> "IVR",
                          "DialplanXml" -> dialplan,
                          "Number" -> number.number.toString
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
      val purchaseMap = Map(
        "209" -> 10,
        "212" -> 10,
        "213" -> 10,
        "214" -> 10,
        "215" -> 10,
        "216" -> 10,
        "217" -> 10,
        "218" -> 10,
        "219" -> 10,
        "224" -> 10,
        "225" -> 10,
        "226" -> 10,
        "228" -> 10,
        "229" -> 10,
        "231" -> 10,
        "234" -> 10,
        "239" -> 10,
        "240" -> 10,
        "242" -> 10,
        "246" -> 10,
        "248" -> 10,
        "250" -> 10,
        "251" -> 10,
        "252" -> 10,
        "253" -> 10,
        "254" -> 10,
        "256" -> 10,
        "260" -> 10,
        "262" -> 10,
        "264" -> 10,
        "267" -> 10,
        "268" -> 10,
        "269" -> 10,
        "270" -> 10,
        "276" -> 10,
        "281" -> 10,
        "284" -> 10,
        "289" -> 10,
        "301" -> 10,
        "302" -> 10,
        "303" -> 10,
        "304" -> 10,
        "305" -> 10,
        "306" -> 10,
        "307" -> 10,
        "308" -> 10,
        "309" -> 10,
        "310" -> 10,
        "312" -> 10,
        "313" -> 10,
        "314" -> 10,
        "315" -> 10,
        "316" -> 10,
        "317" -> 10,
        "318" -> 10,
        "319" -> 10,
        "320" -> 10,
        "321" -> 10,
        "323" -> 10,
        "325" -> 10,
        "330" -> 10,
        "334" -> 10,
        "336" -> 10,
        "337" -> 10,
        "339" -> 10,
        "340" -> 10,
        "345" -> 10,
        "347" -> 10,
        "351" -> 10,
        "352" -> 10,
        "360" -> 10,
        "361" -> 10,
        "386" -> 10,
        "401" -> 10,
        "402" -> 10,
        "403" -> 10,
        "404" -> 10,
        "405" -> 10,
        "406" -> 10,
        "407" -> 10,
        "408" -> 10,
        "409" -> 10,
        "410" -> 10,
        "412" -> 10,
        "413" -> 10,
        "414" -> 10,
        "415" -> 10,
        "416" -> 10,
        "417" -> 10,
        "418" -> 10,
        "419" -> 10,
        "423" -> 10,
        "424" -> 10,
        "425" -> 10,
        "430" -> 10,
        "432" -> 10,
        "434" -> 10,
        "435" -> 10,
        "438" -> 10,
        "440" -> 10,
        "441" -> 10,
        "443" -> 10,
        "450" -> 10,
        "456" -> 10,
        "469" -> 10,
        "473" -> 10,
        "478" -> 10,
        "479" -> 10,
        "480" -> 10,
        "484" -> 10,
        "500" -> 10,
        "501" -> 10,
        "502" -> 10,
        "503" -> 10,
        "504" -> 10,
        "505" -> 10,
        "506" -> 10,
        "507" -> 10,
        "508" -> 10,
        "509" -> 10,
        "510" -> 10,
        "512" -> 10,
        "513" -> 10,
        "514" -> 10,
        "515" -> 10,
        "516" -> 10,
        "517" -> 10,
        "518" -> 10,
        "519" -> 10,
        "520" -> 10,
        "530" -> 10,
        "540" -> 10,
        "541" -> 10,
        "551" -> 10,
        "559" -> 10,
        "561" -> 10,
        "562" -> 10,
        "563" -> 10,
        "567" -> 10,
        "570" -> 10,
        "571" -> 10,
        "573" -> 10,
        "574" -> 10,
        "580" -> 10,
        "585" -> 10,
        "586" -> 10,
        "600" -> 10,
        "601" -> 10,
        "602" -> 10,
        "603" -> 10,
        "604" -> 10,
        "605" -> 10,
        "606" -> 10,
        "607" -> 10,
        "608" -> 10,
        "609" -> 10,
        "610" -> 10,
        "612" -> 10,
        "613" -> 10,
        "614" -> 10,
        "615" -> 10,
        "616" -> 10,
        "617" -> 10,
        "618" -> 10,
        "619" -> 10,
        "620" -> 10,
        "623" -> 10,
        "626" -> 10,
        "630" -> 10,
        "631" -> 10,
        "636" -> 10,
        "641" -> 10,
        "646" -> 10,
        "647" -> 10,
        "649" -> 10,
        "650" -> 10,
        "651" -> 10,
        "660" -> 10,
        "661" -> 10,
        "662" -> 10,
        "664" -> 10,
        "670" -> 10,
        "671" -> 10,
        "678" -> 10,
        "682" -> 10,
        "684" -> 10,
        "700" -> 10,
        "701" -> 10,
        "702" -> 10,
        "703" -> 10,
        "704" -> 10,
        "705" -> 10,
        "706" -> 10,
        "707" -> 10,
        "708" -> 10,
        "709" -> 10,
        "710" -> 10,
        "712" -> 10,
        "713" -> 10,
        "714" -> 10,
        "715" -> 10,
        "716" -> 10,
        "717" -> 10,
        "718" -> 10,
        "719" -> 10,
        "720" -> 10,
        "724" -> 10,
        "727" -> 10,
        "731" -> 10,
        "732" -> 10,
        "734" -> 10,
        "740" -> 10,
        "754" -> 10,
        "757" -> 10,
        "758" -> 10,
        "760" -> 10,
        "762" -> 10,
        "763" -> 10,
        "765" -> 10,
        "767" -> 10,
        "769" -> 10,
        "770" -> 10,
        "772" -> 10,
        "773" -> 10,
        "774" -> 10,
        "775" -> 10,
        "778" -> 10,
        "780" -> 10,
        "781" -> 10,
        "784" -> 10,
        "785" -> 10,
        "786" -> 10,
        "787" -> 10,
        "801" -> 10,
        "802" -> 10,
        "803" -> 10,
        "804" -> 10,
        "805" -> 10,
        "806" -> 10,
        "807" -> 10,
        "808" -> 10,
        "809" -> 10,
        "810" -> 10,
        "812" -> 10,
        "813" -> 10,
        "814" -> 10,
        "815" -> 10,
        "816" -> 10,
        "817" -> 10,
        "818" -> 10,
        "819" -> 10,
        "828" -> 10,
        "829" -> 10,
        "830" -> 10,
        "831" -> 10,
        "832" -> 10,
        "843" -> 10,
        "845" -> 10,
        "847" -> 10,
        "848" -> 10,
        "850" -> 10,
        "856" -> 10,
        "857" -> 10,
        "858" -> 10,
        "859" -> 10,
        "860" -> 10,
        "862" -> 10,
        "863" -> 10,
        "864" -> 10,
        "865" -> 10,
        "867" -> 10,
        "868" -> 10,
        "869" -> 10,
        "870" -> 10,
        "876" -> 10,
        "878" -> 10,
        "900" -> 10,
        "901" -> 10,
        "902" -> 10,
        "903" -> 10,
        "904" -> 10,
        "905" -> 10,
        "906" -> 10,
        "907" -> 10,
        "908" -> 10,
        "909" -> 10,
        "910" -> 10,
        "912" -> 10,
        "913" -> 10,
        "914" -> 10,
        "915" -> 10,
        "916" -> 10,
        "917" -> 10,
        "918" -> 10,
        "919" -> 10,
        "920" -> 10,
        "925" -> 10,
        "928" -> 10,
        "931" -> 10,
        "936" -> 10,
        "937" -> 10,
        "939" -> 10,
        "940" -> 10,
        "941" -> 10,
        "947" -> 10,
        "949" -> 10,
        "951" -> 10,
        "952" -> 10,
        "954" -> 10,
        "956" -> 10,
        "970" -> 10,
        "971" -> 10,
        "972" -> 10,
        "973" -> 10,
        "978" -> 10,
        "979" -> 10,
        "980" -> 10,
        "985" -> 10,
        "989" -> 10
      )

    runBulkOrderPurchase(purchaseMap)
    //val prefix = args(0)
    //val city = args(1)
    //val count = args(2)

    //runTryPurchaseAndConfigureNumber(client, prefix.toInt, city, count.toInt)

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
