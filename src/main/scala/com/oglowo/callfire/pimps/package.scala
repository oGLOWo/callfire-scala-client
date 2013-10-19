package com.oglowo.callfire

import scala.concurrent.{ExecutionContext, Future}
import spray.http.HttpResponse
import spray.httpx.{UnsuccessfulResponseException, PipelineException}
import spray.httpx.unmarshalling._
import scala.language.implicitConversions
import spray.json._
import spray.httpx.SprayJsonSupport
import SprayJsonSupport._
import ExecutionContext.Implicits.global
import com.oglowo.callfire.entity.ApiError
import com.oglowo.callfire.json.ApiEntityFormats._
import spray.httpx.unmarshalling.BasicUnmarshallers._
import com.typesafe.scalalogging.log4j.Logging
import scala.reflect.ClassTag

package object pimps extends Logging {
  implicit class PimpedStringContext(val sc: StringContext) {
    def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }

  implicit class PimpedHttpResponseFuture(val underlying: Future[HttpResponse]) extends AnyVal {
//    def as[T: RootJsonFormat]: Future[T] = underlying.map(response => {
//      if (response.status.isSuccess) {
//        val convertedEntity = response.entity.as[T]
//        if (convertedEntity.isRight) convertedEntity.right.get
//        else throw new PipelineException(convertedEntity.left.get.toString)
//      }
//      else throw new UnsuccessfulResponseException(response)
//    })

    def as[T](implicit unmarshaller: Unmarshaller[T]): Future[T] = underlying.map(response => {
      if (response.status.isSuccess) {
        response.entity.as[T] match {
          case Right(entity) => entity
          case Left(error) => throw new PipelineException(error.toString)
        }
      }
      else throw new UnsuccessfulResponseException(response)
    })
  }

  implicit class PimpedUnsuccessfulResponseException(val underlying: UnsuccessfulResponseException) {
    def asApiError = underlying.response.entity.asString.asJson.convertTo[ApiError] // TODO: What if this craps out in parsing?
  }
}
