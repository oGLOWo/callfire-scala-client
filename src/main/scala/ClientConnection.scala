package com.oglowo.callfire

import akka.actor.{ActorSystem, ActorRef}
import akka.util.Timeout

trait ClientConnection {
  implicit val system: ActorSystem
  implicit val timeout: Timeout
  implicit val connection: ActorRef
}
