package com.oglowo.callfire

import akka.actor.{Props, Actor, ActorRef, ActorSystem}
import akka.util.Timeout
import scala.concurrent.duration._
import spray.util.SprayActorLogging
import scala.util.Random
import spray.can.Http
import spray.http._
import spray.http.ContentTypes._
import spray.http.HttpMethods._
import spray.httpx.encoding.Gzip
import scala.concurrent.ExecutionContext
import scalaz._
import Scalaz._
import spray.http.HttpHeaders._
import spray.http.Uri.Path
import spray.http.HttpRequest
import spray.http.HttpResponse
import spray.http.HttpHeaders.RawHeader
import spray.json._
import pimps._
import com.oglowo.callfire.entity.ApiError
import com.oglowo.callfire.json.ApiEntityFormats._

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

  val credentials: Pair[String, String] = ("", "")

  val connection: ActorRef = system.actorOf(Props(
    new Actor with SprayActorLogging {
      val random = new Random(38)
      var compressNext = random.nextBoolean()

      def statusCodeFromResponseBody(body: String): StatusCode = {
        if (body.contains("ResourceException")) {
          val error = body.asJson.convertTo[ApiError]
          error.httpStatus
        }
        else {
          StatusCodes.OK
        }
      }

      def receive = {
        case request @ HttpRequest(method, Uri(_, _, path, query, _), headers, entity, protocol) =>
          log.debug("Responding to " + request)
          val responseBody: String = path.toString.stripSuffix("/") match {
            case r"/api/1.1/rest/number/18554459732.json$$" => method match {
              case GET => InboundIvrConfiguredNumberGet
              case _ => ???
            }
            case r"/api/1.1/rest/number/12133426826.json$$" => method match {
              case GET => InboundCallTrackingConfiguredNumberGet
              case _ => ???
            }
            case r"/api/1.1/rest/number/(\d+)$phoneNumber\.json$$" => method match {
              case GET => randomNumberGet()
              case _ => ???
            }
            case _ => randomError()
          }

          val response = HttpResponse(
            status = statusCodeFromResponseBody(responseBody),
            entity = HttpEntity(`application/json`, responseBody),
            headers = List(createContentTypeHeader(`application/json`), createContentLengthHeader(entity), createDateHeader(), createVaryHeader()))

          sender ! (if (compressNext) Gzip.encode(response) else response)
          compressNext = random.nextBoolean()
      }
    }), "handler")
}
