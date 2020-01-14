import scala.collection.JavaConverters._

name := "PrivateHome"

version := "0.1"

scalaVersion := "2.13.0"

lazy val akkaVersion = "2.6.0-RC1"

libraryDependencies ++= Seq(
  "com.pi4j" % "pi4j-core" % "1.2",
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.1.10",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalatest" %% "scalatest" % "3.0.8" % Test
)
libraryDependencies += "com.pi4j" % "pi4j-parent" % "1.2"
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.2.0"
libraryDependencies += "org.eclipse.paho" % "mqtt-client" % "0.4.0"
resolvers += "MQTT Repository" at "https://repo.eclipse.org/content/repositories/paho-releases/"