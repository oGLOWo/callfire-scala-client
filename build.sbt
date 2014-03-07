import AssemblyKeys._

assemblySettings

name := "callfire-scala-client"

organization := "com.oglowo"

scalaVersion := "2.10.3"

version := "0.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8", "-language:postfixOps", "-language:implicitConversions", "-Xlint")

scalacOptions in Test ++= Seq("-Yrangepos")

//javaOptions := Seq("-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5006")

resolvers ++= Seq("spray repo" at "http://repo.spray.io/")

fork := false

libraryDependencies ++= {
  val akkaVersion = "2.2.3"
  val sprayVersion = "1.2.0"
  Seq(
    "io.spray"                  %   "spray-can"           %  sprayVersion,
    "io.spray"                  %   "spray-http"          %  sprayVersion,
    "io.spray"                  %   "spray-httpx"         %  sprayVersion,
    "io.spray"                  %   "spray-util"          %  sprayVersion,
    "io.spray"                  %   "spray-testkit"       %  sprayVersion,
    "io.spray"                  %   "spray-client"        %  sprayVersion,
    "com.typesafe.akka"         %%  "akka-actor"          %  akkaVersion,
    "com.typesafe.akka"         %%  "akka-testkit"        %  akkaVersion,
    "org.specs2"                %%  "specs2"              %  "2.2.3"         %  "test",
    "io.spray"                  %%  "spray-json"          %  "1.2.5",
    "org.scalaz"                %%  "scalaz-core"         %  "7.0.2",
    "com.chuusai"               %%  "shapeless"           %  "1.2.4",
    "com.typesafe"              %%  "scalalogging-log4j"  %  "1.0.1",
    "com.github.nscala-time"    %%  "nscala-time"         %  "0.4.2",
    "org.apache.logging.log4j"  %  "log4j-api"            %  "2.0-beta9",
    "org.apache.logging.log4j"  %  "log4j-core"           %  "2.0-beta9",
    "org.joda"                  %  "joda-money"           %  "0.9",
    "com.oglowo"                %% "scala-phonenumber"    %  "0.1"
  )
}
