package com.oglowo

import scala.concurrent.{ExecutionContext, Future}
import spray.http.HttpResponse
import spray.json.RootJsonFormat
import spray.httpx.{UnsuccessfulResponseException, PipelineException}
import spray.httpx.unmarshalling._
import scala.language.implicitConversions
import spray.json._
import spray.httpx.SprayJsonSupport
import SprayJsonSupport._
import ExecutionContext.Implicits.global
import com.oglowo.callfire.entity.ApiError
import com.oglowo.callfire.json.ApiEntityFormats._

package object callfire {
  val DefaultVoicemailRecordingName = "voicemail_recording"
  val DefaultCallRecordingName = "call_recording"
}
