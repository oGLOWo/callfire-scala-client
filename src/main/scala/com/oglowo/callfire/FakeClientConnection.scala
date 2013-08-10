package com.oglowo.callfire

import akka.actor.{Props, Actor, ActorRef, ActorSystem}
import akka.util.Timeout
import scala.concurrent.duration._
import spray.util.SprayActorLogging
import scala.util.Random
import spray.can.Http
import spray.http._
import spray.http.ContentTypes._
import spray.httpx.encoding.Gzip
import spray.http.HttpRequest
import spray.http.HttpResponse
import scala.concurrent.ExecutionContext
import scalaz._
import Scalaz._
import spray.http.HttpHeaders._

trait FakeClientConnection extends ClientConnection with FakeResponses {
  implicit val system: ActorSystem = ActorSystem("fake-callfire-scala-spray-client")
  implicit val context: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = 15.seconds

  def createContentTypeHeader(contentType: ContentType) = `Content-Type`(contentType)
  def createDateHeader(dateTime: DateTime = DateTime.now) = Date(dateTime)
  def createContentLengthHeader(entity: HttpEntity = EmptyEntity) =
    `Content-Length`(entity match {
      case EmptyEntity => 0
      case HttpBody(_, buffer) => buffer.length
    })
  def createVaryHeader(value: String = "User-Agent,Accept-Encoding") = RawHeader("Vary", value)

  val connection: ActorRef = system.actorOf(Props(
    new Actor with SprayActorLogging {
      val random = new Random(38)
      var compressNext = random.nextBoolean()

      def receive = {
        case request @ HttpRequest(method, uri, headers, entity, protocol) =>
          log.debug("Responding to " + request)
          val response = HttpResponse(
            entity = HttpEntity(`application/json`, InboundIvrConfiguredNumberGet),
            headers = List(createContentTypeHeader(`application/json`), createContentLengthHeader(entity), createDateHeader(), createVaryHeader()))

          sender ! (if (compressNext) Gzip.encode(response) else response)
          compressNext = random.nextBoolean()
      }
    }), "handler")
}
