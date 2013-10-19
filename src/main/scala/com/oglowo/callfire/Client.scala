package com.oglowo.callfire

import akka.util.Timeout
import scala.concurrent.{Await, ExecutionContext, Future}
import akka.actor.{ActorSystem}
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
import com.typesafe.scalalogging.log4j.Logging
import spray.httpx.encoding.{Gzip, Deflate}
import spray.http.HttpHeaders._
import pimps._
import scalaz._
import Scalaz._
import scalaz.{Success => ScalazSuccess, Failure => ScalazFailure}
import spray.http.HttpRequest
import scala.util.{Random, Failure, Success}
import scala.Some
import spray.http.HttpResponse
import com.oglowo.callfire.Imports._
import java.util.UUID
import spray.json._
import spray.httpx.SprayJsonSupport
import SprayJsonSupport._
import MediaRanges._
import spray.httpx.unmarshalling.{Unmarshaller, BasicUnmarshallers}
import spray.httpx.marshalling.BasicMarshallers


trait Client {
  this: ClientConnection =>

  val ApiBase = "/api/1.1/rest"

  lazy val log = Logging(system, getClass)

  import system.dispatcher

  def addJoyentCredentials(keyPath: String, fingerprint: String, login: String): RequestTransformer = {
    // compute shit
    val signature = "asdfasdfasdf"
    import spray.httpx._
    addCredentials(GenericHttpCredentials("Signature", Map.empty[String, String]))
  }

  lazy val pipeline: HttpRequest => Future[HttpResponse] = (
    addCredentials(BasicHttpCredentials(credentials._1, credentials._2))
      ~> addJoyentCredentials("", "", "")
      ~> addHeader(`User-Agent`(ProductVersion("Callfire Scala Client", "1.0", "http://github.com/oGLOWo/callfire-scala-client"), ProductVersion("spray-client", "1.2-M8", "http://spray.io")))
      ~> logRequest(log)
      ~> sendReceive(connection)(context, timeout)
      ~> decode(Deflate)
      ~> decode(Gzip)
    )

  private def constructPath(path: String): String = {
    val base = if (ApiBase.endsWith("/")) ApiBase.stripSuffix("/")
    else ApiBase |> {
      s => if (s.startsWith("/")) s else "/" + s
    }
    val trimmedPath = if (path.startsWith("/")) path.stripSuffix("/") else path
    s"$base/$trimmedPath"
  }

  def get(path: String, maybeParameters: Option[Map[String, String]] = None): Future[HttpResponse] = pipeline {
    val endpoint = constructPath(path)
    maybeParameters match {
      case Some(parameters) => {
        println(s"Hitting $endpoint with GET and ${FormData(parameters)}")
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
    val parameters = Map("Numbers" -> numbers.map(_.number.toString).mkString(","))
    post("number/order.json", parameters.some).as[OrderReference]
  }

  def getOrder(orderReference: OrderReference): Future[entity.Order] = {
    get(s"number/order/${orderReference.id}.json").as[entity.Order]
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

  def shutdown(): Unit = {
    IO(Http)(system).ask(Http.CloseAll)(1.seconds).await
    system.shutdown()
  }
}
