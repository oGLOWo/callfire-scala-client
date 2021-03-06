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

case class ProductionClientConnection(actorSystem: ActorSystem, config: Config) extends ClientConnection {
  implicit val system: ActorSystem = actorSystem
  implicit val context: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = 360.seconds

  val credentials: Pair[String, String] = (config.getString("com.oglowo.callfire.username"), config.getString("com.oglowo.callfire.password"))

  def this(system: ActorSystem) = this(system, ConfigFactory.load())

  val connection: ActorRef = {
    for {
      Http.HostConnectorInfo(connector, _) <- IO(Http) ? Http.HostConnectorSetup(config.getString("com.oglowo.callfire.api-host"), port = 443, sslEncryption = true)
    } yield connector
  }.await

  override def shutdown(): Unit = {
    // we don't want to to this since it's not our ActorSystem
    //IO(Http)(system).ask(Http.CloseAll)(1.seconds).await
    //system.shutdown()
  }
}
