package com.oglowo.callfire

import akka.util.Timeout
import scala.concurrent.{Await, ExecutionContext, Future}
import akka.actor.{Cancellable, ActorSystem}
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
import com.typesafe.scalalogging.slf4j.LazyLogging
import spray.httpx.encoding.{Gzip, Deflate}
import spray.http.HttpHeaders._
import pimps._
import scalaz._
import Scalaz._
import scalaz.{Success => ScalazSuccess, Failure => ScalazFailure}
import scala.util.{Random, Failure, Success}
import scala.Some
import com.oglowo.callfire.Imports._
import java.util.UUID
import spray.json._
import spray.httpx.SprayJsonSupport
import SprayJsonSupport._
import MediaRanges._
import spray.httpx.unmarshalling.{Unmarshaller, BasicUnmarshallers}
import spray.httpx.marshalling.BasicMarshallers
import spray.http.MediaTypes._
import spray.http.ContentTypes
import spray.http.HttpRequest
import com.oglowo.callfire.entity.SoundReference
import scala.Some
import com.oglowo.callfire.entity.OrderReference
import com.oglowo.callfire.entity.SoundMetaData
import spray.http.HttpResponse
import spray.http.BodyPart
import java.nio.charset.Charset
import com.typesafe.config.{ConfigFactory, Config}


class Client(clientConnection: ClientConnection, config: Config) {
  def this(clientConnection: ClientConnection) = this(clientConnection, ConfigFactory.load())

  val ApiBase = "/api/1.1/rest"

  lazy val log = Logging(clientConnection.system, getClass)

  import clientConnection.system.dispatcher

  lazy val pipeline: HttpRequest => Future[HttpResponse] = (
    addCredentials(BasicHttpCredentials(clientConnection.credentials._1, clientConnection.credentials._2))
      ~> addHeader(`User-Agent`(ProductVersion("Callfire Scala Client", "0.8.7", "http://github.com/oGLOWo/callfire-scala-client"), ProductVersion("spray-client", "1.3.2", "http://spray.io")))
      ~> logRequest(log)
      ~> sendReceive(clientConnection.connection)(clientConnection.context, clientConnection.timeout)
      ~> decode(Deflate)
      ~> decode(Gzip)
    )

  private def constructPath(path: String): String = {
    val trimmedPath = if (path.startsWith("/")) path.stripSuffix("/") else path
    s"$ApiBase/$trimmedPath"
  }

  def get(path: String, maybeParameters: Option[Map[String, String]] = None): Future[HttpResponse] = pipeline {
    val endpoint = constructPath(path)
    maybeParameters match {
      case Some(parameters) => {
        log.debug(s"Hitting $endpoint with GET and ${FormData(parameters)}")
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

  def multipartPost(path: String, maybeParameters: Option[Map[String, (ContentType, Any)]]): Future[HttpResponse] = pipeline {
    val endpoint = constructPath(path)
    log.debug(s"Posting $maybeParameters to $path")
    maybeParameters match {
      case Some(parameters) => {
        val parts = parameters.mapValues(value => {
          value._2 match {
            case buffer: Array[Byte] => BodyPart(HttpEntity(value._1, buffer))
            case default => BodyPart(HttpEntity(value._1, default.toString))
          }
        })

        Post(endpoint, MultipartFormData(parts)).withHeaders(`Content-Type`(`multipart/form-data`))
      }
      case None => Post(endpoint).withHeaders(`Content-Type`(`multipart/form-data`))
    }
  }

  def multipartPut(path: String, maybeParameters: Option[Map[String, (ContentType, Any)]]): Future[HttpResponse] = pipeline {
    val endpoint = constructPath(path)
    log.debug(s"Posting $maybeParameters to $path")
    maybeParameters match {
      case Some(parameters) => {
        val parts = parameters.mapValues(value => {
          value._2 match {
            case buffer: Array[Byte] => BodyPart(HttpEntity(value._1, buffer))
            case default => BodyPart(HttpEntity(value._1, default.toString))
          }
        })

        Put(endpoint, MultipartFormData(parts)).withHeaders(`Content-Type`(`multipart/form-data`))
      }
      case None => Put(endpoint).withHeaders(`Content-Type`(`multipart/form-data`))
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
    val parameters = Map(
      "Numbers" -> numbers.map(_.number.toString).mkString(",")
    )
    post("number/order.json", parameters.some).as[OrderReference]
  }

  def orderNumbers(localCount: Int, prefix: Option[String] = None): Future[OrderReference] = {
    val parameters = Map(
      "localCount" -> localCount.toString
    ) ++ { prefix match {
      case Some(s) => Map("Prefix" -> s)
      case None => Map.empty
    }}
    post("number/order.json", parameters.some).as[OrderReference]
  }

  def bulkOrderNumbers(prefix: Option[Min4DigitInt] = None, city: Option[String] = None, count: Int = 1): Future[OrderReference] = {
    val parameters: Map[String, String] = Map("localCount" -> count.toString) |>
      { map => if (prefix.isDefined) map + ("Prefix" -> prefix.get.underlying.toString) else map } |>
      { map => if (city.isDefined) map + ("City" -> city.get) else map }
    post("number/order.json", parameters.some).as[OrderReference]
  }

  def getOrder(orderReference: OrderReference): Future[entity.Order] = {
    get(s"number/order/${orderReference.id}.json").as[entity.Order]
  }

  def getOrder(orderId: Long): Future[entity.Order] = {
    get(s"number/order/$orderId.json").as[entity.Order]
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

  def getNumber(number: PhoneNumber): Future[PhoneNumber]  = {
    get(s"number/${number.number}.json").as[PhoneNumber]
  }

  def configureNumber(number: PhoneNumber): Future[PhoneNumber] = {
    number.configuration match {
      case Some(configuration) => {
        println("Configuration is good")
        val parts: Map[String, (ContentType, Any)] =
          { if (configuration.callFeature.isDefined) Map(("CallFeature", (ContentTypes.`text/plain`, configuration.callFeature.get.value))) else Map.empty[String, (ContentType, Any)] } |>
          { m: Map[String, (ContentType, Any)] => if (configuration.textFeature.isDefined) m + ("TextFeature" -> (ContentTypes.`text/plain`, configuration.textFeature.get.value)) else m } |>
          { m: Map[String, (ContentType, Any)] =>
            configuration.inboundCallConfiguration match {
              case Some(inboundConfiguration) => inboundConfiguration match {
                case configuration: InboundIvrConfiguration => {
                  m + ("InboundCallConfigurationType" -> (ContentTypes.`text/plain`, "IVR")) +
                    ("IvrInboundConfig[id]" -> (ContentTypes.`text/plain`, configuration.id.get)) +
                    ("DialplanXml" -> (ContentType(`application/xml`), configuration.dialplanXml.get.toString.getBytes(Charset.forName("UTF-8"))))
                }
                case default => throw new IllegalArgumentException(s"$default is not currently supported by the client")
              }
              case None => m
            }
          }
        multipartPut(s"number/${number.number}.json", parts.some).flatMap(_ => Future(number))
      }
      case None => {
        Future { number }
      }
    }
  }

  def recordSoundViaPhone(number: PhoneNumber, maybeName: Option[String] = None): Future[SoundReference] = {
    val soundName = maybeName match {
      case Some(name) => s"$name-Vonjour-${number.number}-${UUID.randomUUID()}"
      case None => s"Vonjour-${number.number}-${UUID.randomUUID()}"
    }

    val parameters = Map(
      "ToNumber" -> s"${number.number}",
      "Name" -> soundName
    )

    post("call/sound.json", parameters.some).as[SoundReference]
  }

  def getSoundMetaData(id: Long): Future[SoundMetaData] = {
    get(s"call/sound/$id.json").as[SoundMetaData]
  }

  def getSoundMetaData(reference: SoundReference): Future[SoundMetaData] = {
    getSoundMetaData(reference.id)
  }

  def getSound(reference: SoundReference, soundType: SoundType = WavSoundType): Future[Array[Byte]] = {
    getSound(reference.id, soundType)
  }

  def getSound(id: Long, soundType: SoundType): Future[Array[Byte]] = {
    val extension = soundType match {
      case WavSoundType => "wav"
      case Mp3SoundType => "mp3"
    }
    get(s"call/sound/$id.$extension").as(BasicUnmarshallers.ByteArrayUnmarshaller)
  }

  def getRecordingSoundFromCall(recordingName: String, call: Call, soundType: SoundType = Mp3SoundType): Future[Array[Byte]] = {
    val extension = soundType match {
      case Mp3SoundType => "mp3"
      case WavSoundType =>  "wav"
    }

    call.containsRecording(recordingName) match {
      case true => get(s"call/${call.id}/$recordingName.$extension").as(BasicUnmarshallers.ByteArrayUnmarshaller)
      case false => throw new IllegalArgumentException(s"Call $call does not contain a recording with name '$recordingName'")
    }
  }

  def getVoicemailSoundFromCall(call: Call, soundType: SoundType = Mp3SoundType): Future[Array[Byte]] = getRecordingSoundFromCall(DefaultVoicemailRecordingName, call, soundType)

  def getCallRecordingSoundFromCall(call: Call, soundType: SoundType = Mp3SoundType): Future[Array[Byte]] = getRecordingSoundFromCall(DefaultCallRecordingName, call, soundType)

  def getCall(id: Long): Future[Call] = {
    get(s"call/$id.json").as[Call]
  }

  // bigMessageStrategy - None means CallFire will use their default which is currently send multiple
  def sendText(to: PhoneNumber, message: String, from: Option[PhoneNumber] = None, bigMessageStrategy: Option[BigMessageStrategy] = None): Future[TextBroadcastReference] = {
    val parameters =
      { Map("To" -> to.number.toString, "Message" -> message) } |>
      { m => if (from.isDefined) m + ("From" -> from.get.number.toString) else m } |>
      { m => if (bigMessageStrategy.isDefined) m + ("BigMessageStrategy" -> bigMessageStrategy.get.name) else m }

    post("text.json", parameters.some).as[TextBroadcastReference]
  }

  def getTextByBroadcastId(broadcastId: Long): Future[Text] = {
    val parameters = Map("BroadcastId" -> broadcastId.toString, "MaxResults" -> 1.toString)
    get("text.json", parameters.some).as[Seq[Text]].map(_.head)
  }

  def shutdown(): Unit = clientConnection.shutdown()
}
