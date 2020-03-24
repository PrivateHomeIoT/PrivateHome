package PrivateHome

import PrivateHome.Devices.MHz.mhzSwitch
import PrivateHome.Devices.MQTT.mqttSwitch
import PrivateHome.Devices.Switch
import scalikejdbc._


object dataSQL {

  var devices:Map[String, Switch] = Map()


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
  ConnectionPool.singleton("jdbc:h2:./daten/hello", "user", "pass")

  implicit val session: AutoSession.type = AutoSession
  // for now, retrieves all data as Map value


  /**
   * Generates the Table strucktures for the Database
   * @return a Boolean defining the succes of the Creation
   */
  def create(): Unit = {

    // Drops old Tables to Delet Data and make it possible to regenerate them
    sql"""DROP TABLE IF EXISTS `Mhz`;
         DROP TABLE IF EXISTS `Devices`;""".execute().apply()


    val devices = sql"""
                    CREATE TABLE `Devices` (
                    `id` varchar(5) NOT NULL,
                    `name` varchar(64) NOT NULL,
                    `type` varchar(16) NOT NULL,
                    `state` decimal(5,4) NOT NULL,
                    `keepState` boolean NOT NULL,
                    PRIMARY KEY (`id`))
         """.execute.apply()



    sql"""
           CREATE TABLE `Mhz` (
           `id` varchar(5) NOT NULL,
           `systemcode` varchar(5) NOT NULL,
           `unitcode` varchar(5) NOT NULL,
           PRIMARY KEY (`id`),
           CONSTRAINT `Mhz_ibfk_1` FOREIGN KEY (`id`) REFERENCES `Devices` (`id`))
         """.execute().apply()


    // insert initial data


  }


  /**
   *
   * @return
   */
  def getDevices(): Unit = {
    val m = Device.syntax("m")
    withSQL {
      select.from(Device as m)
    }.map(rs => Device(rs)).list().apply().foreach(d => {
      d.switchtype match {
        case "MQTT" => devices.+((d.id, mqttSwitch(d.id, keepStatus = d.keepState)))
          devices(d.id).on(d.state)
        case "433Mhz" =>
          val m = Mhz.syntax("m")
          val data = withSQL{ select.from(Mhz as m).where.eq(m.id,d.id)}.map(rs => Mhz(rs)).single().apply().get
          devices + ((d.id,mhzSwitch(d.id,d.keepState,data.systemcode,data.unitcode)))
          devices(d.id).on(d.state)
      }

    })

  }



  def addDevice(device: Switch): Unit = {
    var wrongclass = new IllegalArgumentException("""Can not add Switch ID:{} because switch of unknown type {} has no save definition""".format(device.id, device.getClass))
    withSQL {
      insertInto(Device).values(device.id, "", device.switchtype, if (device.KeepStatus) device.Status else 0, device.KeepStatus)
    }.update.apply
    device match {
      case device: mhzSwitch => withSQL {
        insertInto(Mhz).values(device.id, device.systemCode, device.unitCode)
      }.update().apply()
      case _: mqttSwitch =>
      case _ => throw wrongclass
    }
  }

  case class Device(id: String, name: String, switchtype: String, state: Float, keepState: Boolean)

  case class Mhz(id: String, systemcode: String, unitcode: String)

  object Device extends SQLSyntaxSupport[Device] {
    override val tableName = "devices"

    def apply(rs: WrappedResultSet) = new Device(
      rs.string("id"), rs.string("name"), rs.string("type"), rs.float("state"), rs.boolean("keepState"))
  }

  object Mhz extends SQLSyntaxSupport[Mhz] {
    override val tableName = "Mhz"

    def apply(rs: WrappedResultSet): Mhz = new Mhz(rs.string("id"), rs.string("systemcode"), rs.string("unitcode"))
  }

}