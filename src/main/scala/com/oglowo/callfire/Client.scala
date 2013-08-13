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

trait Client {
  this: ClientConnection =>
  lazy val log = Logging(system, getClass)

//  def logRequest(log: LoggingAdapter): HttpRequest ⇒ HttpRequest =
//    logRequest { request ⇒ log.debug(request.toString) }
  import system.dispatcher
  lazy val pipeline: HttpRequest => Future[HttpResponse] = {
    addHeader(`User-Agent`(ProductVersion("Callfire Scala Client", "1.0", "http://github.com/oGLOWo/callfire-scala-client"), ProductVersion("spray-client", "1.2-M8", "http://spray.io"))) ~>
    logRequest(log) ~>
    sendReceive(connection)(context, timeout) ~>
    decode(Deflate) ~>
    decode(Gzip)
  }

  def get(path: String): Future[HttpResponse] = pipeline {
    Get(path)
  }

  def post(path: String, parameters: Map[String, String]): Future[HttpResponse] = pipeline {
    Post(path, FormData(parameters))
  }

  def shutdown(): Unit = {
    IO(Http)(system).ask(Http.CloseAll)(1.seconds).await
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