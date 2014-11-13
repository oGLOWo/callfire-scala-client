import SonatypeKeys._

sonatypeSettings

name := "callfire-scala-client"

organization := "com.oglowo"

version := "0.8.5"

description := "CallFire Scala client sdk for hitting REST endpoints and generating CallFireXML Call Control data"

startYear :=  Some(2013)

homepage      := Some(url("https://github.com/oGLOWo/callfire-scala-client"))

organizationHomepage := Some(url("http://oGLOWo.com"))

licenses      := Seq(("Apache 2.0", url("http://www.apache.org/licenses/LICENSE-2.0")))

scmInfo       := Some(ScmInfo(url("https://github.com/oGLOWo/callfire-scala-client"), "scm:git:https://github.com/oGLOWo/callfire-scala-client.git", Some("scm:git:git@github.com:oGLOWo/callfire-scala-client.git")))

scalaVersion  := "2.10.4"

crossScalaVersions := Seq("2.10.0", "2.10.1", "2.10.2", "2.10.3", "2.10.4")

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8", "-language:postfixOps", "-language:implicitConversions", "-Xlint")

scalacOptions in Test ++= Seq("-Yrangepos")

useGpg := true

publishMavenStyle := true

publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
          Some("snapshots" at nexus + "content/repositories/snapshots")
    else
          Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra :=
  <developers>
    <developer>
      <id>oGLOWo</id>
      <name>Adrian Rodriguez</name>
      <url>http://oGLOWo.com</url>
    </developer>
  </developers>

resolvers ++= Seq("spray repo" at "http://repo.spray.io/")

libraryDependencies ++= {
  val akkaVersion = "2.3.0"
  val sprayVersion = "1.3.1"
  Seq(
    "io.spray"                    %   "spray-can"            %  sprayVersion,
    "io.spray"                    %   "spray-http"           %  sprayVersion,
    "io.spray"                    %   "spray-httpx"          %  sprayVersion,
    "io.spray"                    %   "spray-util"           %  sprayVersion,
    "io.spray"                    %   "spray-testkit"        %  sprayVersion    %  "test",
    "io.spray"                    %   "spray-client"         %  sprayVersion,
    "com.typesafe.akka"           %%  "akka-actor"           %  akkaVersion,
    "com.typesafe.akka"           %%  "akka-testkit"         %  akkaVersion     %  "test",
    "org.specs2"                  %%  "specs2-core"          %  "2.3.7"         %  "test",
    "io.spray"                    %%  "spray-json"           %  "1.2.6",
    "org.scalaz"                  %%  "scalaz-core"          %  "7.0.2",
    "com.chuusai"                 %%  "shapeless"            %  "1.2.4",
    "com.typesafe.scala-logging"  %%  "scala-logging-slf4j"  %  "2.1.2",
    "com.github.nscala-time"      %%  "nscala-time"          %  "0.4.2",
    "org.joda"                    %   "joda-money"           %  "0.9",
    "com.oglowo"                  %%  "scala-phonenumber"    %  "0.3"
  )
}
