package com.oglowo.callfire

import akka.actor.{Props, Actor, ActorRef, ActorSystem}
import akka.util.Timeout
import scala.concurrent.duration._
import spray.util.SprayActorLogging
import scala.util.Random
import spray.can.Http
import spray.http._
import spray.httpx.encoding.Gzip
import spray.http.HttpRequest
import spray.http.HttpResponse
import scala.concurrent.ExecutionContext

trait FakeClientConnection extends ClientConnection {
  object Implicits {
    implicit val system: ActorSystem = ActorSystem("fake-callfire-scala-spray-client")
    implicit val context: ExecutionContext = system.dispatcher
    implicit val timeout: Timeout = 15.seconds
  }

  val system = Implicits.system
  val context = Implicits.context
  val timeout = Implicits.timeout
  val connection: ActorRef = system.actorOf(Props(
    new Actor with SprayActorLogging {
      var dropNext = true
      val random = new Random(38)
      def receive = {
        case _: Http.Connected ⇒ sender ! Http.Register(self)
        case HttpRequest(_, Uri.Path("/compressedResponse"), _, _, _) ⇒ {
          sender ! Gzip.encode(HttpResponse(entity = "content"))
        }
        case x: HttpRequest if x.uri.toString.startsWith("/drop1of2") && dropNext ⇒
          log.debug("Dropping " + x)
          dropNext = random.nextBoolean()
        case x @ HttpRequest(method, uri, _, entity, _) ⇒
          println("Responding to " + x)
          dropNext = random.nextBoolean()
          val mirroredHeaders = x.header[HttpHeaders.`User-Agent`].toList
          sender ! HttpResponse(entity = method + "|" + uri.path + (if (entity.isEmpty) "" else "|" + entity.asString), headers = mirroredHeaders)
        case Timedout(request)         ⇒ sender ! HttpResponse(entity = "TIMEOUT")
        case ev: Http.ConnectionClosed ⇒ log.debug("Received " + ev)
      }
    }), "handler")
}
