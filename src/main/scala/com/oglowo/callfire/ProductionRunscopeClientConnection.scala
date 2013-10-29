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

trait ProductionRunscopeClientConnection extends ClientConnection {
  implicit val system: ActorSystem = ActorSystem("callfire-scala-spray-client")
  implicit val context: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = 360.seconds

  // DO NOT commit and push the credentials out to github since this bitch is open source.
  // You don't want to give the world access to your CallFire account because that might
  // run up your bill quite a bit. If you do push it out, go to the callfire dashboard
  // and remove these credentials from API Access and create a new set.
  val credentials: Pair[String, String] = ("8eccf6f02069", "1dd1705ba4fb8bb2")

  val connection: ActorRef = {
    for {
      // Don't commit and push the host name with the actual bucket key otherwise you're giving the world access
      // to your runscope account since this bitch is open source.
      // If you happen to push it out, do yourself a favor and create a new bucket and delete the one associated
      // with the key you pushed.
      Http.HostConnectorInfo(connector, _) <- IO(Http) ? Http.HostConnectorSetup("www-callfire-com-b5mwzph4hatx.runscope.net", port = 443, sslEncryption = true)
    } yield connector
  }.await
}