package PrivateHome

import PrivateHome.Devices.MHz.mhzSwitch
import PrivateHome.Devices.MQTT.mqttSwitch
import PrivateHome.Devices.Switch
import scalikejdbc._


object dataSQL {


  GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(
    enabled = false,
    singleLineMode = false,
    printUnprocessedStackTrace = false,
    stackTraceDepth = 15,
    logLevel = Symbol("debug"),
    warningEnabled = true,
    warningThresholdMillis = 3000L,
    warningLogLevel = Symbol("warn")
  )


  Class.forName("org.h2.Driver")
  ConnectionPool.singleton("jdbc:h2:./daten:hello", "user", "pass")

  implicit val session: AutoSession.type = AutoSession
  // for now, retrieves all data as Map value

  def create(): Boolean = {
    sql"""
          create table devices (
          id varchar(10) not null primary key,
          name varchar(64),
          type varchar(16) not null,
          state decimal(1,4) not null default false,
          keepState boolean not null default false
          )
         """.execute.apply()
    // insert initial data

    sql"""
           create table 433Mhz (
           "id" varchar(10) not null primary key,
           "systemcode" int(5) not null,
           "unitcode" int(5) not null,
           constraint primary foreign key ('id') references 'devices' ('id')
           )
         """.execute().apply()
  }

  def getDevices: Option[Devices] = {
    val m = Devices.syntax("m")
    withSQL {
      select.from(Devices as m)
    }.map(rs => Devices(rs)).single().apply()
  }


  def addDevice(device: Switch): Unit = {
    var wrongclass = new IllegalArgumentException("""Can not add Switch ID:{} because switch of unknown type {} has no save definition""".format(device.id, device.getClass))
    withSQL {
      insertInto(Devices).values(device.id(), "", device.switchtype, if (device.KeepStatus) device.Status else 0, device.KeepStatus)
    }.update.apply
    device match {
      case device: mhzSwitch => withSQL {
        insertInto(Mhz).values(device.id, device.systemCode, device.unitCode)
      }.update().apply()
      case _: mqttSwitch =>
      case _ => throw wrongclass
    }
  }

  case class Devices(id: String, name: String, switchtype: String, state: Float, keepState: Boolean)

  case class Mhz(id: String, systemcode: Int, unitcode: Int)

  object Devices extends SQLSyntaxSupport[Devices] {
    override val tableName = "devices"

    def apply(rs: WrappedResultSet) = new Devices(
      rs.string("id"), rs.string("name"), rs.string("type"), rs.float("state"), rs.boolean("keepState"))
  }

  object Mhz extends SQLSyntaxSupport[Mhz] {
    override val tableName = "433Mhz"

    def apply(rs: WrappedResultSet): Mhz = new Mhz(rs.string("id"), rs.int("systemcode"), rs.int("unitcode"))
  }

}