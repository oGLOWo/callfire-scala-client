package com.oglowo.callfire

import akka.util.Timeout
import scala.concurrent.{ExecutionContext, Future}
import akka.actor.{ActorSystem}
import akka.pattern.ask
import akka.io.IO
import scala.util.{Failure, Success}
import spray.http._
import spray.httpx.UnsuccessfulResponseException
import spray.can.Http
import spray.client.pipelining._
import spray.util._
import scala.concurrent.duration._
import com.oglowo.callfire.entity.{ApiError, PhoneNumber}
import com.oglowo.callfire.json.ApiEntityFormats._
import akka.event.Logging
import com.typesafe.scalalogging.log4j.Logging
import spray.httpx.encoding.{Gzip, Deflate}
import spray.http.HttpHeaders._
import pimps._
import scalaz._
import Scalaz._

trait Client {
  this: ClientConnection =>
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

  def get(path: String, maybeParameters: Option[Map[String, String]] = None): Future[HttpResponse] = pipeline {
    maybeParameters match {
      case Some(parameters) => Get(path, FormData(parameters))
      case None => Get(path)
    }
  }

  def post(path: String, maybeParameters: Option[Map[String, String]] = None): Future[HttpResponse] = pipeline {
    maybeParameters match {
      case Some(parameters) => Post(path, FormData(parameters))
      case None => Post(path)
    }
  }

  def put(path: String, maybeParameters: Option[Map[String, String]] = None): Future[HttpResponse] = pipeline {
    maybeParameters match {
      case Some(parameters) => Put(path, FormData(parameters))
      case None => Put(path)
    }
  }

  def patch(path: String, maybeParameters: Option[Map[String, String]] = None): Future[HttpResponse] = pipeline {
    maybeParameters match {
      case Some(parameters) => Patch(path, FormData(parameters))
      case None => Patch(path)
    }
  }

  def delete(path: String, maybeParameters: Option[Map[String, String]] = None): Future[HttpResponse] = pipeline {
    maybeParameters match {
      case Some(parameters) => Delete(path, FormData(parameters))
      case None => Delete(path)
    }
  }

  def head(path: String, maybeParameters: Option[Map[String, String]] = None): Future[HttpResponse] = pipeline {
    maybeParameters match {
      case Some(parameters) => Head(path, FormData(parameters))
      case None => Head(path)
    }
  }

  def options(path: String, maybeParameters: Option[Map[String, String]] = None): Future[HttpResponse] = pipeline {
    maybeParameters match {
      case Some(parameters) => Options(path, FormData(parameters))
      case None => Options(path)
    }
  }

  def shutdown(): Unit = {
    IO(Http)(system).ask(Http.CloseAll)(1.seconds).await
    system.shutdown()
  }
}

object Main extends Logging {
  def main(args: Array[String]) {
    val prefix = args(0)
    val count = args(1)
    val purchaseCount = args(2)


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

        println("Pop in that number you just ordered: ")
        val number = readLine()
        client.put(s"/api/1.1/rest/number/$number.json", Some(Map(
          "CallFeature" -> "ENABLED",
          "TextFeature" -> "DISABLED",
          "InboundCallConfigurationType" -> "IVR",
          "DialplanXml" -> dialplan,
          "Number" -> number)
        )) onComplete {
          case Success(response) => {
            logger.info("Response was: {} ... now getting the info", response.status)
            client.get(s"/api/1.1/rest/number/$number.json").as[PhoneNumber] onComplete {
              case Success(phoneNumber) => {
                logger.info("The phone number is {}", phoneNumber)
                println(s"YOUR PHONE NUMBER ${phoneNumber.nationalFormat} IS NOW READY TO USE! Call it, foo!")
                client.shutdown()
              }
              case Failure(error) => {
                error match {
                  case e: UnsuccessfulResponseException => logger.info("API ERROR {}", e.asApiError)
                  case e: Throwable => logger.error("BOOOOO NON API ERROR", e)
                }
                client.shutdown()
              }
            }
          }
          case Failure(error) => {
            error match {
              case e: UnsuccessfulResponseException => logger.info("API ERROR {}", e.asApiError)
              case e: Throwable => logger.error("BOOOOO NON API ERROR", e)
            }
            client.shutdown()
        }
      }
    }

//    client.put("/api/1.1/rest/number/12133426857.json", Some(Map(
//      "CallFeature" -> "ENABLED",
//      "TextFeature" -> "DISABLED",
//      "InboundCallConfigurationType" -> "IVR",
//      "DialplanXml" -> dialplan,
//      "Number" -> "12133426857")
//    )) onComplete {
//      case Success(response) => {
//        logger.info("Response was: {} ... now getting the info", response.status)
//        client.get("/api/1.1/rest/number/12133426857.json").as[PhoneNumber] onComplete {
//          case Success(phoneNumber) => {
//            logger.info("The phoen number is {}", phoneNumber)
//            client.shutdown()
//          }
//          case Failure(error) => {
//            error match {
//              case e: UnsuccessfulResponseException => logger.info("API ERROR {}", e.asApiError)
//              case e: Throwable => logger.error("BOOOOO NON API ERROR", e)
//            }
//            client.shutdown()
//          }
//        }
//      }
//      case Failure(error) => {
//        error match {
//          case e: UnsuccessfulResponseException => logger.info("API ERROR {}", e.asApiError)
//          case e: Throwable => logger.error("BOOOOO NON API ERROR", e)
//        }
//        client.shutdown()
//      }
//    }
  }