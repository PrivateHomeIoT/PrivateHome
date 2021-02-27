package PrivateHome

import PrivateHome.Devices.MHz.mhzSwitch
import PrivateHome.Devices.MQTT.mqttSwitch
import PrivateHome.Devices.{Switch, switchSerializer}
import PrivateHome.UI.Websocket.websocket
import org.h2.jdbc.JdbcSQLNonTransientConnectionException
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization.write
import org.json4s.jackson.{JsonMethods, Serialization}
import org.json4s.{Formats, NoTypeHints}
import scalikejdbc._

import scala.collection.mutable


object data {
  /**
   * An map containing all settings
   */
  var devices: mutable.Map[String, Switch] = mutable.Map()


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
  ConnectionPool.singleton("jdbc:h2:" + settings.database.path, settings.database.userName, settings.database.passwordString)

  implicit val session: AutoSession.type = AutoSession
  var mhzId: mutable.Map[String, String] = mutable.Map()
  try {
    if (sql"""show tables;""".map(rs => rs).list.apply().isEmpty)
      create()
  } catch {
    case e:JdbcSQLNonTransientConnectionException => Console.err.println(Console.RED + e.getMessage)
      sys.exit(75)
    case e:Throwable => e.printStackTrace(Console.err)
  }
  fillDevices()


  /**
   * Generates the table structures for the Database
   */
  def create(dropTables: Boolean = false): Unit = {

    // Drops old Tables to Delete Data and make it possible to regenerate them
    if (dropTables) {
      sql"""DROP TABLE IF EXISTS `Mhz`;
         DROP TABLE IF EXISTS `Devices`;
         DROP TABLE IF EXISTS `User`""".execute().apply()
      devices = mutable.Map()
    }


    val devices =
      sql"""
                    CREATE TABLE IF NOT EXISTS `Devices` (
                    `id` varchar(5) NOT NULL,
                    `name` varchar(64) NOT NULL,
                    `switchtype` varchar(16) NOT NULL,
                    `state` decimal(5,4) NOT NULL,
                    `keepstate` boolean NOT NULL,
                    `controltype` varchar(16),
                    PRIMARY KEY (`id`))
         """.execute.apply()


    sql"""
           CREATE TABLE IF NOT EXISTS `Mhz` (
           `id` varchar(5) NOT NULL,
           `systemcode` varchar(5) NOT NULL,
           `unitcode` varchar(5) NOT NULL,
           PRIMARY KEY (`id`),
           CONSTRAINT `Mhz_ibfk_1` FOREIGN KEY (`id`) REFERENCES `Devices` (`id`))
         """.execute().apply()

    sql"""
         CREATE TABLE IF NOT EXISTS `User` (
         `username` varchar(30) NOT NULL,
         `passhash` varchar(100) NOT NULL,
         PRIMARY KEY (`username`))
       """.execute().apply()

    sql"""
         SHOW Tables;
       """.execute().apply().toString


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
        case "mqtt" => devices += ((d.id, mqttSwitch(d.id, d.keepstate, d.name, d.controlType)))
          if (d.keepstate) {
            devices(d.id).on(d.state)
          }
        case "433Mhz" =>
          val m = Mhz.syntax("m")
          val data = withSQL {
            select.from(Mhz as m).where.eq(m.id, d.id)
          }.map(rs => Mhz(rs)).single().apply().get
          devices += ((d.id, mhzSwitch(d.id, d.keepstate, d.name, data.systemcode, data.unitcode)))
          mhzId += ((data.systemcode + data.unitcode, d.id))
          if (d.keepstate) {
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
      insertInto(Device).values(device.id, device.name, device.switchtype, if (device.keepStatus) device.status else 0, device.keepStatus, device.controlType)
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
    websocket.broadcastMsg(("Command" -> "newDevice") ~ ("answer" -> JsonMethods.parse(write(device))))
  }


  /**
   * Adds a user to the database
   *
   * @param username The username to Store
   * @param passHash The argon2 hash of the password (encoded
   */
  def addUser(username: String, passHash: String): Unit = {
    withSQL {
      insertInto(user).values(username, passHash)
    }.update().apply()
  }


  /**
   * Gets the argon2 hash for the Username
   *
   * @param username the username for which to get the hash
   * @return the hash of the User of there is no User with this name returns "no_Username"
   */
  def getUserHash(username: String): String = {
    val m = user.syntax("m")
    var value: String = null
    withSQL {
      select.from(user as m).where.eq(user.column.username, username)
    }.map(rs => user(rs)).single().apply().foreach(u => {
      if (u == null) {
        value = "no_Username"
      } else
        value = u.passhash
    })
    if (value == null) {
      "no_Username"
    } else value
  }

  /**
   * Saves the Status of an switch for the keepStatus function
   *
   * @param id    the ID of the switch to change
   * @param state the new state
   */
  def saveStatus(id: String, state: Float): Unit = {
    withSQL {
      update(Device).set(Device.column.state -> state).where.eq(Device.column.id, id)
    }.update().apply()
  }

  //ToDo: add support for Settingschanges

  def idTest(id: String, create: Boolean = false): Unit = {
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
   * @param id          ID of the Device in the Format [0-9a-Z] five character long
   * @param name        The name of the Device any String lenght in the Table 64 character
   * @param switchtype  A String identification of the Switch type max length 16 character
   * @param state       an foatingpoint representation of the State when keepState is true 4 decimalpoints long
   * @param keepstate   Boolean that indicates if the State should be restored at turn on
   * @param controlType either slider or button
   */
  case class Device(id: String, name: String, switchtype: String, state: Float, keepstate: Boolean, controlType: String)

  /**
   * Message class for Mhz Table only needed when Switchtype is MQTT
   *
   * @param id         ID of the Device in the Format [0-9a-Z] five character long
   * @param systemcode Systemcode for the Mhzreciever
   * @param unitcode   Unitcode for the Mhzreciever
   */
  case class Mhz(id: String, systemcode: String, unitcode: String)

  /**
   * Message class for user Table
   *
   * @param username the username
   * @param passhash the argon2id hash
   */
  case class user(username: String, passhash: String)

  /**
   * Syntax support Object for devices table
   */
  object Device extends SQLSyntaxSupport[Device] {
    override val tableName = "devices"

    def apply(rs: WrappedResultSet) = new Device(
      rs.string("id"), rs.string("name"), rs.string("switchtype"), rs.float("state"), rs.boolean("keepState"), rs.string("controlType"))
  }

  /**
   * Syntax support Object for Mhz table
   */
  object Mhz extends SQLSyntaxSupport[Mhz] {
    override val tableName = "Mhz"

    def apply(rs: WrappedResultSet): Mhz = new Mhz(rs.string("id"), rs.string("systemcode"), rs.string("unitcode"))
  }

  /**
   * Syntax support Object for user table
   */
  object user extends SQLSyntaxSupport[user] {
    override val tableName = "User"

    def apply(rs: WrappedResultSet): user = new user(rs.string("username"), rs.string("passhash"))
  }

}

