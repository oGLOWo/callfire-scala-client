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
import com.typesafe.config.{ConfigFactory, Config}

case class StandaloneProductionRunscopeClientConnection(config: Config) extends ClientConnection {
  implicit val system: ActorSystem = ActorSystem("callfire-scala-spray-client")
  implicit val context: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = 360.seconds

  def this() = this(ConfigFactory.load())

  val credentials: Pair[String, String] = (config.getString("com.oglowo.callfire.username"), config.getString("com.oglowo.callfire.password"))

  val connection: ActorRef = {
    for {
      Http.HostConnectorInfo(connector, _) <- IO(Http) ? Http.HostConnectorSetup(config.getString("com.oglowo.callfire.runscope-api-host"), port = 443, sslEncryption = true)
    } yield connector
  }.await

  def shutdown(): Unit = {
    IO(Http)(system).ask(Http.CloseAll)(1.seconds).await
    system.shutdown()
  }
}
