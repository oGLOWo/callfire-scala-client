package com.oglowo.callfire

import akka.actor.{ActorSystem, ActorRef}
import akka.util.Timeout
import scala.concurrent.ExecutionContext
import spray.http.HttpCredentials
import akka.io.IO
import spray.can.Http

trait ClientConnection {
  val context: ExecutionContext
  val system: ActorSystem
  val timeout: Timeout
  val connection: ActorRef
  val credentials: Pair[String, String]

  def shutdown(): Unit
}
