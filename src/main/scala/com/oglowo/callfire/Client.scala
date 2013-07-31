package com.oglowo.callfire

import akka.util.Timeout
import java.util.{Date, UUID}
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.event.Logging
import akka.io.IO
import scala.util.{Failure, Success}
import spray.http.{BasicHttpCredentials, HttpRequest, HttpResponse}
import spray.httpx.UnsuccessfulResponseException
import spray.can.Http
import spray.client.pipelining._
import spray.util._
import scala.concurrent.duration._
import com.oglowo.callfire.entity.{ApiError, PhoneNumber}
import com.oglowo.callfire.json.ApiEntityFormats._

object Client {
  implicit val system = ActorSystem("callfire-scala-spray-client")
  implicit val timeout: Timeout = 15.seconds
  import system.dispatcher

  val log = Logging(system, getClass)

  val connection = {
    for {
      Http.HostConnectorInfo(connector, _) <- IO(Http) ? Http.HostConnectorSetup("www.callfire.com", port = 443, sslEncryption = true)
    } yield connector
  }.await

  def debugRequest(request: HttpRequest): Unit = {
    log.debug("!!!!! -> {} {} {}", request.uri, request.entity, request.message)
    log.debug(request.toString)
  }

  def pipeline = addCredentials(BasicHttpCredentials("89459683b251", "c950391d0b9a89d5")) ~> logRequest(debugRequest _) ~> sendReceive(connection)

  def get(path: String): Future[HttpResponse] = pipeline {
    Get(path)
  }

  def shutdown(): Unit = {
    IO(Http).ask(Http.CloseAll)(1.seconds).await
    system.shutdown()
  }

  def main(args: Array[String]) {
    val path = args(0)
    log.info("Requesting path {}", path)

    get(path) onComplete {
      case Success(response) => {
        log.info("The response is {}", response)
        shutdown()
      }
      case Failure(error) => {
        error match {
          case e: UnsuccessfulResponseException => log.info("API ERROR {}", e.asApiError)
          case e: Throwable => log.error(e, "BOOO NON API ERROR")
        }
        shutdown()
      }
    }
  }
}