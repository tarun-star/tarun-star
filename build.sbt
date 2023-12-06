ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "ReactiveChatApp"
  )

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.6.15",
  "com.typesafe.akka" %% "akka-stream" % "2.6.15",
  "com.typesafe.akka" %% "akka-http" % "10.2.6",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.2.6",
  "com.typesafe.akka" %% "akka-actor-typed" % "2.6.15",
  "com.typesafe.play" %% "play-akka-http-server" % "2.8.10"
)
