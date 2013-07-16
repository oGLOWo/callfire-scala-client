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
import spray.http.HttpResponse
import spray.httpx.{UnsuccessfulResponseException}
import spray.can.Http
import spray.client.pipelining._
import spray.util._
import scala.concurrent.duration._
import com.oglowo.callfire.entity.{User, ApiError}
import com.oglowo.callfire.json.ApiEntityFormats._

object Client {
  implicit val system = ActorSystem("callfire-scala-spray-client")
  implicit val timeout: Timeout = 15.seconds
  import system.dispatcher

  val log = Logging(system, getClass)

  val connection = {
    for {
      Http.HostConnectorInfo(connector, _) <- IO(Http) ? Http.HostConnectorSetup("localhost", port = 8080)
    } yield connector
  }.await

  def pipeline: SendReceive = sendReceive(connection)

  def get[T](path: String): Future[HttpResponse] = pipeline {
    Get(path)
  }

  def shutdown(): Unit = {
    IO(Http).ask(Http.CloseAll)(1.second).await
    system.shutdown()
  }

  def main(args: Array[String]) {
    val userId = args(0)
    log.info("Requesting some user id {}", userId)

    get(s"/users/$userId").as[User] onComplete {
      case Success(user) => {
        log.info("The user is {}", user)
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