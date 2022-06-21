import com.typesafe.sbt.packager.debian.DebianPlugin.autoImport.DebianConstants._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
name := "PrivateHome"

//version := "0.1"

scalaVersion := "2.13.6"

maintainer := "RaHoni <honisuess@gmail.com>"

packageSummary := "This is a SmartHome system"

packageDescription := "This is a SmartHome project focused on design, security and expandability. It is programmed in Scala."

debianPackageDependencies  := Seq("java8-runtime-headless","mosquitto")
debianPackageRecommends := Seq("wiringpi", "pi4j (<< 2.0.0)")
linuxPackageMappings ++= Seq(
  packageMapping(file(s"debiansettings.json") -> s"/etc/${normalizedName.value}/settings.json").withUser(normalizedName.value).withGroup(normalizedName.value).withConfig(),
  packageMapping(file(s"src/debian/etc/mosquitto/conf.d/privatehome.conf") -> s"/etc/mosquitto/conf.d/privatehome.conf"),
  packageTemplateMapping(s"/etc/${normalizedName.value}/data")().withUser(normalizedName.value).withGroup(normalizedName.value),
  packageTemplateMapping(s"/var/log/${normalizedName.value}")().withUser(normalizedName.value).withGroup(normalizedName.value)
)
releaseIgnoreUntrackedFiles := true
Global / onChangedBuildSource := ReloadOnSourceChanges

cleanKeepFiles += target.value / "scala-2.13" / "scoverage-report"

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "PrivateHome"
  )

releaseProcess := Seq[ReleaseStep](
  inquireVersions,
  setReleaseVersion,
  runClean,
  runTest,
  releaseStepCommand("debian:packageBin"),
  setNextVersion
)

//version in Debian := "0.1-20201211-8"

Debian / maintainerScripts  := maintainerScriptsAppendFromFile((Debian/maintainerScripts ).value)(
  Postinst ->  sourceDirectory.value / "debian" / "postinst"
)

lazy val akkaVersion = "2.6.17"

//pi4j java wrapper for WiringPI (deprecated by author may be continued be other)
libraryDependencies ++= Seq("com.pi4j" % "pi4j-core" % "1.2","com.pi4j" % "pi4j-parent" % "1.2")

//scala Test also used by Akka
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.2" % Test

//logback logger used by Akka and scalikejdbc and the whole project
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.6"

//Akka library for Webserver with Websockets Support
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.2.0",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.2.2" % Test
)

//XML library for scala
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.2.0"

//MQTT library
resolvers += "MQTT Repository" at "https://repo.eclipse.org/content/repositories/paho-releases/"
libraryDependencies += "org.eclipse.paho" % "mqtt-client" % "0.4.0"

//scalikejdbc library for mysql (backend h2)
libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc" % "3.4.0",
  "org.scalikejdbc" %% "scalikejdbc-test" % "3.4.0" % "test",
  "com.h2database" % "h2" % "1.4.200",
  "org.scalikejdbc" %% "scalikejdbc-config" % "3.4.0")

libraryDependencies += "org.json4s" % "json4s-jackson_2.13" % "3.6.7"

libraryDependencies += "de.mkammerer" % "argon2-jvm" % "2.9.1"

libraryDependencies += "org.scala-sbt.ipcsocket" % "ipcsocket" % "1.3.0"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"


//scallop libary for cmdline parsing
libraryDependencies += "org.rogach" %% "scallop" % "4.0.4"

//sbt-native-packaging Plugins for compiling to deb
enablePlugins(DebianPlugin)
enablePlugins(JavaServerAppPackaging)
enablePlugins(SystemdPlugin)
