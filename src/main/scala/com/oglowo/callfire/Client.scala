package com.oglowo.callfire

import akka.util.Timeout
import java.util.{Date, UUID}
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor.{ActorRef, ActorSystem}
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

object Client extends ClientConnection {
  this: ActorSystem with Timeout with ActorRef =>
  val log = Logging(system, getClass)
//  implicit val system = ActorSystem("callfire-scala-spray-client")
//  implicit val timeout: Timeout = 15.seconds
  import system.dispatcher
//

//
//  val connection = {
//    for {
//      Http.HostConnectorInfo(connector, _) <- IO(Http) ? Http.HostConnectorSetup("www.callfire.com", port = 443, sslEncryption = true)
//    } yield connector
//  }.await
//
//  step {
//    val testService = system.actorOf(Props(
//      new Actor with SprayActorLogging {
//        var dropNext = true
//        val random = new Random(38)
//        def receive = {
//          case _: Http.Connected ⇒ sender ! Http.Register(self)
//          case HttpRequest(_, Uri.Path("/compressedResponse"), _, _, _) ⇒
//            sender ! Gzip.encode(HttpResponse(entity = "content"))
//          case x: HttpRequest if x.uri.toString.startsWith("/drop1of2") && dropNext ⇒
//            log.debug("Dropping " + x)
//            dropNext = random.nextBoolean()
//          case x @ HttpRequest(method, uri, _, entity, _) ⇒
//            log.debug("Responding to " + x)
//            dropNext = random.nextBoolean()
//            val mirroredHeaders = x.header[HttpHeaders.`User-Agent`].toList
//            sender ! HttpResponse(entity = method + "|" + uri.path + (if (entity.isEmpty) "" else "|" + entity.asString), headers = mirroredHeaders)
//          case Timedout(request)         ⇒ sender ! HttpResponse(entity = "TIMEOUT")
//          case ev: Http.ConnectionClosed ⇒ log.debug("Received " + ev)
//        }
//      }), "handler")
//    IO(Http).ask(Http.Bind(testService, interface, port))(3.seconds).await
//  }


  def debugRequest(request: HttpRequest): Unit = {
    log.debug("!!!!! -> {} {} {}", request.uri, request.entity, request.message)
    log.debug(request.toString)
  }

  def pipeline =
    addCredentials(BasicHttpCredentials("", "")) ~> logRequest(debugRequest _) ~> sendReceive(connection)

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

    get(path).as[PhoneNumber] onComplete {
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