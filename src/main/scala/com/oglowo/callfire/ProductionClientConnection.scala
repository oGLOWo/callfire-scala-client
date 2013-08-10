package com.oglowo.callfire

import spray.can.Http
import akka.io.IO
import akka.actor.{ActorSystem, ActorRef}
import akka.pattern.ask
import spray.util._
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

trait ProductionClientConnection extends ClientConnection {
  implicit val system: ActorSystem = ActorSystem("callfire-scala-spray-client")
  implicit val context: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = 15.seconds

  val connection: ActorRef = {
    for {
      Http.HostConnectorInfo(connector, _) <- IO(Http) ? Http.HostConnectorSetup("www.callfire.com", port = 443, sslEncryption = true)
    } yield connector
  }.await
}