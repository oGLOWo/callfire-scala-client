import AssemblyKeys._

assemblySettings

name := "callfire-scala-client"

organization := "com.oglowo"

scalaVersion := "2.10.1"

version := "0.1"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

scalacOptions in Test ++= Seq("-Yrangepos")

javaOptions := Seq("-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5006")

resolvers ++= Seq("spray repo" at "http://repo.spray.io/")

libraryDependencies ++= Seq(
  "io.spray"                  %   "spray-can"           %  "1.2-M8",
  "io.spray"                  %   "spray-http"          %  "1.2-M8",
  "io.spray"                  %   "spray-httpx"         %  "1.2-M8",
  "io.spray"                  %   "spray-util"          %  "1.2-M8",
  "io.spray"                  %   "spray-testkit"       %  "1.2-M8",
  "io.spray"                  %   "spray-client"        %  "1.2-M8",
  "com.typesafe.akka"         %%  "akka-actor"          %  "2.2.0-RC1",
  "com.typesafe.akka"         %%  "akka-testkit"        %  "2.2.0-RC1",
  "org.specs2"                %%  "specs2"              %  "2.0"         %  "test",
  "io.spray"                  %%  "spray-json"          %  "1.2.5",
  "org.scalaz"                %%  "scalaz-core"         %  "7.0.2",
  "com.chuusai"               %%  "shapeless"           %  "1.2.4",
  "com.typesafe"              %%  "scalalogging-log4j"  %  "1.0.1",
  "com.github.nscala-time"    %%  "nscala-time"         %  "0.4.2",
  "org.apache.logging.log4j"  %  "log4j-api"            %  "2.0-beta8",
  "org.apache.logging.log4j"  %  "log4j-core"           %  "2.0-beta8"
)