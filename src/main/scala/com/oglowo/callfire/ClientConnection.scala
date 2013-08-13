package com.oglowo.callfire

import akka.actor.{ActorSystem, ActorRef}
import akka.util.Timeout
import scala.concurrent.ExecutionContext
import spray.http.HttpCredentials

trait ClientConnection {
  val context: ExecutionContext
  val system: ActorSystem
  val timeout: Timeout
  val connection: ActorRef
  val credentials: Option[Pair[String, String]]
}
