package com.oglowo.callfire

import spray.can.Http
import akka.io.IO
import akka.actor.{ActorSystem, ActorRef}
import akka.pattern.ask
import spray.util._
import akka.util.Timeout
import akka.util.Timeout._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

trait ProductionClientConnection extends ClientConnection {
  implicit val system: ActorSystem = ActorSystem("callfire-scala-spray-client")
  implicit val context: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = 360.seconds

  val credentials: Pair[String, String] = (system.settings.config.getString("com.oglowo.callfire.username"), system.settings.config.getString("com.oglowo.callfire.password"))

  val connection: ActorRef = {
    for {
      Http.HostConnectorInfo(connector, _) <- IO(Http) ? Http.HostConnectorSetup(system.settings.config.getString("com.oglowo.callfire.api-host"), port = 443, sslEncryption = true)
    } yield connector
  }.await
}