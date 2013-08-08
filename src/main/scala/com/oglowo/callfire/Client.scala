package com.oglowo.callfire

import akka.util.Timeout
import scala.concurrent.{ExecutionContext, Future}
import akka.actor.{ActorSystem}
import akka.pattern.ask
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
import com.typesafe.scalalogging.slf4j.{Logging, Logger}
import akka.event.Logging
import com.typesafe.scalalogging.slf4j.Logging

trait Client {
  this: ClientConnection =>

  implicit lazy val theContext = context
  implicit lazy val theTimeout = timeout
  implicit lazy val theSystem = system
  lazy val log = Logging(theSystem, getClass)

  def debugRequest(request: HttpRequest): Unit = {
    log.debug("!!!!! -> {} {} {}", request.uri, request.entity, request.message)
    log.debug(request.toString)
  }

  def pipeline = {

    addCredentials(BasicHttpCredentials("8eccf6f02069", "1dd1705ba4fb8bb2")) ~> logRequest(debugRequest _) ~> sendReceive(connection)
  }

  def get(path: String): Future[HttpResponse] = pipeline {
    Get(path)
  }

  def shutdown(): Unit = {
    IO(Http).ask(Http.CloseAll)(1.seconds).await
    system.shutdown()
  }
}

object Main extends Logging {
  def main(args: Array[String]) {
    val path = args(0)
    logger.info("Requesting path {}", path)
    val client = new Client with FakeClientConnection
    import client._

    client.get(path).as[PhoneNumber] onComplete {
      case Success(response) => {
        logger.info("The response is {}", response)
        client.shutdown()
      }
      case Failure(error) => {
        error match {
          case e: UnsuccessfulResponseException => logger.info("API ERROR {}", e.asApiError)
          case e: Throwable => logger.error("BOOO NON API ERROR", e)
        }
        client.shutdown()
      }
    }
  }
}