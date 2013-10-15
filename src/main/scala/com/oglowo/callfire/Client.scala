package com.oglowo.callfire

import akka.util.Timeout
import scala.concurrent.{Await, ExecutionContext, Future}
import akka.actor.{ActorSystem}
import akka.pattern.ask
import akka.io.IO
import spray.http._
import spray.httpx.UnsuccessfulResponseException
import spray.can.Http
import spray.client.pipelining._
import spray.util._
import scala.concurrent.duration._
import com.oglowo.callfire.entity._
import com.oglowo.callfire.json.ApiEntityFormats._
import akka.event.Logging
import com.typesafe.scalalogging.log4j.Logging
import spray.httpx.encoding.{Gzip, Deflate}
import spray.http.HttpHeaders._
import pimps._
import scalaz._
import Scalaz._
import scalaz.{Success => ScalazSuccess, Failure => ScalazFailure}
import spray.http.HttpRequest
import scala.util.{Random, Failure, Success}
import scala.Some
import spray.http.HttpResponse
import com.oglowo.callfire.Imports._


trait Client {
  this: ClientConnection =>

  val ApiBase = "/api/1.1/rest"

  lazy val log = Logging(system, getClass)

  //  def logRequest(log: LoggingAdapter): HttpRequest ⇒ HttpRequest =
  //    logRequest { request ⇒ log.debug(request.toString) }

  import system.dispatcher

  lazy val pipeline: HttpRequest => Future[HttpResponse] = (
    addCredentials(BasicHttpCredentials(credentials._1, credentials._2))
      ~> addHeader(`User-Agent`(ProductVersion("Callfire Scala Client", "1.0", "http://github.com/oGLOWo/callfire-scala-client"), ProductVersion("spray-client", "1.2-M8", "http://spray.io")))
      ~> logRequest(log)
      ~> sendReceive(connection)(context, timeout)
      ~> decode(Deflate)
      ~> decode(Gzip)
    )

  private def constructPath(path: String): String = {
    val base = if (ApiBase.endsWith("/")) ApiBase.stripSuffix("/")
    else ApiBase |> {
      s => if (s.startsWith("/")) s else "/" + s
    }
    val trimmedPath = if (path.startsWith("/")) path.stripSuffix("/") else path
    s"$base/$trimmedPath"
  }

  def get(path: String, maybeParameters: Option[Map[String, String]] = None): Future[HttpResponse] = pipeline {
    val endpoint = constructPath(path)
    maybeParameters match {
      case Some(parameters) => {
        println(s"Hitting $endpoint with GET and ${FormData(parameters)}")
        Get(endpoint, FormData(parameters))
      }
      case None => Get(endpoint)
    }
  }

  def post(path: String, maybeParameters: Option[Map[String, String]] = None): Future[HttpResponse] = pipeline {
    val endpoint = constructPath(path)
    log.debug(s"Posting $maybeParameters to $path")
    maybeParameters match {
      case Some(parameters) => Post(endpoint, FormData(parameters))
      case None => Post(endpoint)
    }
  }

  def put(path: String, maybeParameters: Option[Map[String, String]] = None): Future[HttpResponse] = pipeline {
    val endpoint = constructPath(path)
    maybeParameters match {
      case Some(parameters) => Put(endpoint, FormData(parameters))
      case None => Put(endpoint)
    }
  }

  def patch(path: String, maybeParameters: Option[Map[String, String]] = None): Future[HttpResponse] = pipeline {
    val endpoint = constructPath(path)
    maybeParameters match {
      case Some(parameters) => Patch(endpoint, FormData(parameters))
      case None => Patch(endpoint)
    }
  }

  def delete(path: String, maybeParameters: Option[Map[String, String]] = None): Future[HttpResponse] = pipeline {
    val endpoint = constructPath(path)
    maybeParameters match {
      case Some(parameters) => Delete(endpoint, FormData(parameters))
      case None => Delete(endpoint)
    }
  }

  def head(path: String, maybeParameters: Option[Map[String, String]] = None): Future[HttpResponse] = pipeline {
    val endpoint = constructPath(path)
    maybeParameters match {
      case Some(parameters) => Head(endpoint, FormData(parameters))
      case None => Head(endpoint)
    }
  }

  def options(path: String, maybeParameters: Option[Map[String, String]] = None): Future[HttpResponse] = pipeline {
    val endpoint = constructPath(path)
    maybeParameters match {
      case Some(parameters) => Options(endpoint, FormData(parameters))
      case None => Options(endpoint)
    }
  }

  def orderNumbers(numbers: Set[PhoneNumber]): Future[OrderReference] = {
    val parameters = Map("Numbers" -> numbers.map(_.number.toString).mkString(","))
    post("number/order.json", parameters.some).as[OrderReference]
  }

  def getOrder(orderReference: OrderReference): Future[entity.Order] = {
    get(s"number/order/${orderReference.id}.json").as[entity.Order]
  }

  def searchForNumbers(prefix: Option[Min4DigitInt], city: Option[String], maxNumbers: Int = 1): Future[Seq[PhoneNumber]] = {
    val parameters = Map("Count" -> maxNumbers.toString) |> {
      m => if (prefix.isDefined) m + ("Prefix" -> implicitly[String](prefix.get)) else m
    } |> {
      m => if (city.isDefined) m + ("City" -> city.get.toString) else m
    }
    get("number/search.json", parameters.some).as[Seq[PhoneNumber]]
  }

  def searchForTollFreeNumbers(prefix: Option[Min4DigitInt], maxNumbers: Int = 1): Future[Seq[PhoneNumber]] = {
    val parameters = Map(
      "Count" -> maxNumbers.toString,
      "TollFree" -> true.toString
    ) |> {
      m => if (prefix.isDefined) m + ("Prefix" -> implicitly[String](prefix.get)) else m
    }
    get("number/search.json", parameters.some).as[Seq[PhoneNumber]]
  }

//  def recordSoundViaPhone(number: PhoneNumber, maybeName: Option[String] = None): Future[SoundMetaData] = {
//    val soundName = maybeName match {
//      case Some(name) => name
//      case None => s"Vonjour-CallFire-API-Recording-${number.number}"
//    }
//
//    val parameters = Map(
//      "ToNumber" -> s"${number.number}",
//      "Name" -> soundName
//    )
//
//    post("call/sound.json", parameters.some).as[SoundMetaData]
//  }

  def shutdown(): Unit = {
    IO(Http)(system).ask(Http.CloseAll)(1.seconds).await
    system.shutdown()
  }
}

//curl -u 8eccf6f02069:1dd1705ba4fb8bb2 https://www.callfire.com/api/1.1/rest/call/sound.json -d "ToNumber=+12134485916" > out.txt

object Main extends Logging {
  def main(args: Array[String]) {
    def printError(error: Throwable) = {
      error match {
        case e: UnsuccessfulResponseException => println(s"!!>> API ERROR ${e.asApiError}")
        case e: Throwable =>
          println(s"!!!>>>!!! NON API ERROR")
          e.printStackTrace()

      }
    }
    val prefix = args(0)
    val city = args(1)
    val count = args(2)

    val client = new Client with ProductionClientConnection
    import client._

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
    client.searchForNumbers(implicitly[Min4DigitInt](prefix.toInt).some, city.some, count.toInt) onComplete {
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
                    theOrder = Await.result(client.getOrder(orderReference), Duration.Inf)
                    println("[[[....checking order status....]]]")
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
}
