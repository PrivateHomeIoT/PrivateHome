package PrivateHome

import PrivateHome.Devices.MHz.mhzSwitch
import PrivateHome.Devices.MQTT.mqttSwitch
import PrivateHome.Devices.{Switch, switchSerializer}
import PrivateHome.UI.Websocket.websocket
import org.json4s.{Formats, NoTypeHints}
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization.write
import org.json4s.jackson.{JsonMethods, Serialization}
import scalikejdbc._


object data {
  /**
   * An map containing all settings
   */
  var devices: Map[String, Switch] = Map()


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
  ConnectionPool.singleton("jdbc:h2:"+settings.database.path, settings.database.userName, settings.database.password)

  implicit val session: AutoSession.type = AutoSession
  var mhzId: Map[String, String] = Map()
  if(sql"""show tables;""".map(rs => rs).list.apply().isEmpty) create()
  fillDevices()


  /**
   * Generates the table structures for the Database
   */
  def create(): Unit = {

    // Drops old Tables to Delete Data and make it possible to regenerate them
    sql"""DROP TABLE IF EXISTS `Mhz`;
         DROP TABLE IF EXISTS `Devices`;""".execute().apply()


    val devices =
      sql"""
                    CREATE TABLE `Devices` (
                    `id` varchar(5) NOT NULL,
                    `name` varchar(64) NOT NULL,
                    `type` varchar(16) NOT NULL,
                    `state` decimal(5,4) NOT NULL,
                    `keepState` boolean NOT NULL,
                    `controltype` varchar(16),
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
   * Fills the devices Map with all Devices from the DB and turns all Switches with keepState on
   */
  def fillDevices(): Unit = {
    val m = Device.syntax("m")
    withSQL {
      select.from(Device as m)
    }.map(rs => Device(rs)).list().apply().foreach(d => {
      d.switchtype match {
        case "MQTT" => devices += ((d.id, mqttSwitch(d.id, d.keepState,d.name,d.controlType)))
          if (d.keepState) {
            devices(d.id).on(d.state)
          }
        case "433Mhz" =>
          val m = Mhz.syntax("m")
          val data = withSQL {
            select.from(Mhz as m).where.eq(m.id, d.id)
          }.map(rs => Mhz(rs)).single().apply().get
          devices += ((d.id, mhzSwitch(d.id, d.keepState,d.name, data.systemcode, data.unitcode)))
          mhzId += ((data.systemcode + data.unitcode, d.id))
          if (d.keepState) {
            devices(d.id).on(d.state)
          }
      }

    })

  }

  /**
   * Adds an Switch to the DB witch all needed under Tables
   *
   * @param device The device that should be added
   */
  def addDevice(device: Switch): Unit = {
    var wrongclass = new IllegalArgumentException("""Can not add Switch ID:{} because switch of unknown type {} has no save definition""".format(device.id, device.getClass))
    withSQL {
      insertInto(Device).values(device.id, device.name, device.switchtype, if (device.keepStatus) device.Status else 0, device.keepStatus,device.controlType)
    }.update.apply
    device match {
      case device: mhzSwitch => withSQL {
        insertInto(Mhz).values(device.id, device.systemCode, device.unitCode)
      }.update().apply()
        mhzId += ((device.systemCode + device.unitCode, device.id))
      case _: mqttSwitch =>
      case _ => throw wrongclass
    }
    devices = devices.concat(Map((device.id, device)))
    implicit val formats: Formats = Serialization.formats(NoTypeHints) + new switchSerializer
    websocket.broadcastMsg(("Command" -> "newSwitch") ~ ("Answer" -> JsonMethods.parse(write(device))))
  }

  /**
   * Saves the Status of an switch for the keepStatus function
   * @param id the ID of the switch to change
   * @param state the new state
   */
  def saveStatus(id: String, state: Float): Unit = {
    val d = Device.syntax("d")
    withSQL {
      update(Device).set(Device.column.state -> state).where.eq(Device.column.id, id)
    }.update().apply()
  }

  //ToDo: add support for Settingschanges

  def idTest(id: String,create:Boolean=false): Unit = {
    if (id.length != 5) throw new IllegalArgumentException("""Length of ID is not 5""")
    if (!id.matches("[-_a-zA-Z0-9]{5}")) throw new IllegalArgumentException("""ID Contains not Allowed Characters""")
    if (create && devices.contains(id)) throw new IllegalArgumentException("""ID is already used""")
  }

  /**
   * An better access to the devices Map
   *
   * @param deviceID The ID of the switch you want
   * @return the Switch object the you requested
   */
  def getDevice(deviceID: String): Switch = {
    devices(deviceID)
  }

  /**
   * Message class for devices Table
   *
   * @param id         ID of the Device in the Format [0-9a-Z] five character long
   * @param name       The name of the Device any String lenght in the Table 64 character
   * @param switchtype A String identification of the Switch type max length 16 character
   * @param state      an foatingpoint representation of the State when keepState is true 4 decimalpoints long
   * @param keepState  Boolean that indicates if the State should be restored at turn on
   */
  case class Device(id: String, name: String, switchtype: String, state: Float, keepState: Boolean,controlType: String)

  /**
   * Message class for Mhz Table only needed when Switchtype is MQTT
   *
   * @param id         ID of the Device in the Format [0-9a-Z] five character long
   * @param systemcode Systemcode for the Mhzreciever
   * @param unitcode   Unitcode for the Mhzreciever
   */
  case class Mhz(id: String, systemcode: String, unitcode: String)

  /**
   * Syntaxsupport Object for devices table
   */
  object Device extends SQLSyntaxSupport[Device] {
    override val tableName = "devices"

    def apply(rs: WrappedResultSet) = new Device(
      rs.string("id"), rs.string("name"), rs.string("type"), rs.float("state"), rs.boolean("keepState"),rs.string("controlType"))
  }

  /**
   * Syntaxsupport Object for Mhz table
   */
  object Mhz extends SQLSyntaxSupport[Mhz] {
    override val tableName = "Mhz"

    def apply(rs: WrappedResultSet): Mhz = new Mhz(rs.string("id"), rs.string("systemcode"), rs.string("unitcode"))
  }

}

