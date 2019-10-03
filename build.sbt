name := "PrivateHome"

version := "0.1"

scalaVersion := "2.13.0"

libraryDependencies ++= Seq (
  "com.pi4j" % "pi4j-core" % "1.2"
  )
libraryDependencies += "com.pi4j" % "pi4j-parent" % "1.2"
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.2.0"
libraryDependencies += "org.eclipse.paho" % "mqtt-client" % "0.4.0"
resolvers += "MQTT Repository" at "https://repo.eclipse.org/content/repositories/paho-releases/"