import scala.collection.JavaConverters._

name := "PrivateHome"

version := "0.1"

scalaVersion := "2.13.0"

lazy val akkaVersion = "2.6.8"

libraryDependencies ++= Seq(
  "com.pi4j" % "pi4j-core" % "1.2",
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.2.0",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalatest" %% "scalatest" % "3.2.2" % Test
)
libraryDependencies += "com.pi4j" % "pi4j-parent" % "1.2"
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.2.0"
libraryDependencies += "org.eclipse.paho" % "mqtt-client" % "0.4.0"
resolvers += "MQTT Repository" at "https://repo.eclipse.org/content/repositories/paho-releases/"
libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc"       % "3.4.0",
  "org.scalikejdbc" %% "scalikejdbc-test" % "3.4.0"   % "test",
  "com.h2database"  %  "h2"                % "1.4.200",
  "org.scalikejdbc" %% "scalikejdbc-config"  % "3.4.0",
  "ch.qos.logback"  %  "logback-classic"   % "1.2.3")

libraryDependencies += "org.json4s" % "json4s-jackson_2.13" % "3.6.7"