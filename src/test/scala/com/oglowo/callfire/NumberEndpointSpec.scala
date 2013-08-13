package com.oglowo.callfire

import org.specs2.mutable._
import spray.http._
import org.specs2.specification.Scope
import com.oglowo.callfire.entity._
import com.oglowo.callfire.pimps._
import scalaz._
import Scalaz._
import spray.httpx.UnsuccessfulResponseException
import com.oglowo.callfire.json.ApiEntityFormats._
import com.oglowo.callfire.entity.PhoneNumber
import scala.util.Success
import scala.util.Failure
import com.oglowo.callfire.entity.ApiError

class NumberEndpointSpec extends Specification {
  "Number Endpoint" should {
    "parse a valid number configured with inbound IVR" in new Data with Credentials {
      var result: Either[ApiError, PhoneNumber] = Left(ApiError("Client-Side Default result", StatusCodes.RequestTimeout))

      val client = new Client with FakeClientConnection
      import client._

      client.get(s"/api/1.1/rest/number/$IvrPhoneNumber.json").as[PhoneNumber] onComplete {
        case Success(response) => result = Right(response)
        case Failure(error) => result = error match {
          case e: UnsuccessfulResponseException => Left(e.asApiError)
          case e: Throwable => failure(e.getMessage)
        }
      }

      result must eventually(10, 100.milliseconds) {
        beRight.like {
          case number => number.configuration.get.inboundCallConfiguration.get.inboundCallConfigurationType must beEqualTo(IvrConfigurationType)
        }
      }
    }

    "parse a valid number configured with Call Tracking" in new Data with Credentials {
      var result: Either[ApiError, PhoneNumber] = Left(ApiError("Client-Side Default result", StatusCodes.RequestTimeout))

      val client = new Client with FakeClientConnection
      import client._
      client.get(s"/api/1.1/rest/number/$CallTrackingPhoneNumber.json").as[PhoneNumber] onComplete {
        case Success(response) => result = Right(response)
        case Failure(error) => result = error match {
          case e: UnsuccessfulResponseException => Left(e.asApiError)
          case e: Throwable => failure(e.getMessage)
        }
      }

      result must eventually(10, 100.milliseconds) {
        beRight.like {
          case number => number.configuration.get.inboundCallConfiguration.get.inboundCallConfigurationType must beEqualTo(CallTrackingConfigurationType)
        }
      }
    }

    "be able to update a number with a call tracking configuration" in new Data with Credentials {

    }
  }

  trait Credentials extends Scope {
    val ApiUser = "8eccf6f02069"
    val ApiPassword = "1dd1705ba4fb8bb2"
  }

  trait Data extends Scope {
    val CallTrackingPhoneNumber = "12133426826"
    val IvrPhoneNumber = "18554459732"
  }
}
