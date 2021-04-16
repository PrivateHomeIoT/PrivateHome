/*
 * Privatehome
 *     Copyright (C) 2021  RaHoni honisuess@gmail.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package PrivateHome

import PrivateHome.Devices.MHz.mhzSwitch
import PrivateHome.Devices.MQTT.{mqttController, mqttSwitch}
import PrivateHome.Devices.{Switch, switchSerializer}
import PrivateHome.UI.Websocket.websocket
import PrivateHome.UI.commandUpdateDevice
import org.h2.jdbc.JdbcSQLNonTransientConnectionException
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization.write
import org.json4s.jackson.{JsonMethods, Serialization}
import org.json4s.{Formats, NoTypeHints}
import org.slf4j.LoggerFactory
import scalikejdbc._

import java.io.ByteArrayInputStream
import java.lang.Integer.parseInt
import java.math.BigInteger
import java.security.SecureRandom
import java.sql.PreparedStatement
import scala.collection.mutable

object data {
  def masterIds: List[String] = controller.keys.toList


  private val logger = LoggerFactory.getLogger(this.getClass)

  val chars: Array[Char] = (('0' to '9') ++ ('a' to 'z') ++ ('A' to 'Z') ++ Vector('-', '_')).toArray

  /**
   * An map containing all settings
   */
  var devices: mutable.Map[String, Switch] = mutable.Map()

  private var controller: Map[String, mqttController] = Map()
  private var controllerRandom: mutable.Map[String, mqttController] = mutable.Map()


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
  val conn = ConnectionPool.singleton("jdbc:h2:" + settings.database.path, settings.database.userName, settings.database.passwordString)

  implicit val session: AutoSession.type = AutoSession
  var mhzId: mutable.Map[String, String] = mutable.Map()
  val tablesWanted: List[String] = List("DEVICES", "MHZ", "MQTT", "MQTTCONTROLLER", "USER")
  try {
    val tables = sql"""show tables;""".map(rs => rs.string(1)).list.apply()
    var allTablesExisting = true
    tablesWanted.foreach(t => allTablesExisting = tables.contains(t) && allTablesExisting)
    if (tables.isEmpty || !allTablesExisting) {
      logger.info(s"Because not all Tables do exists we will add all missing with data.create()")
      create()
    }
  } catch {
    case e: JdbcSQLNonTransientConnectionException => logger.warn(e.getMessage)
      sys.exit(75)
    case e: Throwable => logger.error("Unknown error in database init", e)
  }
  fillDevices()


  /**
   * Generates the table structures for the Database
   */
  def create(dropTables: Boolean = false): Unit = {

    // Drops old Tables to Delete Data and make it possible to regenerate them
    if (dropTables) {
      sql"""DROP TABLE IF EXISTS `Mhz`;
         DROP TABLE IF EXISTS `mqttController`;
         DROP TABLE IF EXISTS `mqtt`;
         DROP TABLE IF EXISTS `Devices`;
         DROP TABLE IF EXISTS `User`""".execute().apply()
      devices = mutable.Map()
    }


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
         CREATE TABLE IF NOT EXISTS `mqttController` (
         `masterid` varchar(10) NOT NULL,
         `name` varchar(64) NOT NULL,
         `key` BINARY(16) NOT NULL,
         PRIMARY KEY (`masterid`))
       """.execute().apply()

    sql"""
         CREATE TABLE IF NOT EXISTS `mqtt` (
         `id` varchar(5) NOT NULL,
         `masterid` varchar(10) NOT NULL,
         `port` int NOT NULL,
         PRIMARY KEY (`id`),
         CONSTRAINT `mqtt_1` FOREIGN KEY (`id`) REFERENCES `Devices` (`id`),
         CONSTRAINT `mqtt_2` FOREIGN KEY (`masterid`) REFERENCES `mqttController` (`masterid`))
       """.execute().apply()


    sql"""
         CREATE TABLE IF NOT EXISTS `User` (
         `username` varchar(30) NOT NULL,
         `passhash` varchar(100) NOT NULL,
         PRIMARY KEY (`username`))
       """.execute().apply()

    println(
      sql"""
         SHOW Tables;
       """.map(rs => rs.string(1)).list().apply())


    // insert initial data


  }

  def getControllerRandomCode(code: String): mqttController = {
    controllerRandom(code)
  }

  def getControllerMasterId(masterId: String): mqttController = {
    controller(masterId)
  }

  def controllerNewRandom(tmpController: mqttController): String = {
    var randomCode = ""
    val t = 50 % 15
    val oldCode = tmpController.randomCode
    do {
      val tmp = new BigInteger(10 * 6, new SecureRandom()).toString(2)
      tmp.grouped(6).foreach(t => {
        randomCode += chars(parseInt(t, 2) % 62)
      })
    } while (controllerRandom.contains(randomCode))
    controllerRandom += ((randomCode, tmpController))
    controllerRandom -= oldCode
    randomCode
  }

  def masterIdExists(masterId: String): Boolean = {
    controller.contains(masterId)
  }

  def addController(newController: mqttController, key: Array[Byte]): Unit = {
    newController.setupClient()
    controller += ((newController.masterID, newController))
    val in = new ByteArrayInputStream(key)
    val bytesBinder = ParameterBinder(
      value = in,
      binder = (stmt: PreparedStatement, idx: Int) => stmt.setBinaryStream(idx, in, key.length)
    )
    withSQL {
      insertInto(mqttControllerData).values(newController.masterID, newController.name, bytesBinder)
    }.update().apply()
  }

  /**
   * Fills the devices Map with all Devices from the DB and turns all Switches with keepState on
   */
  def fillDevices(): Unit = {
    val m = device.syntax("m")

    withSQL {
      selectFrom(mqttControllerData as mqttControllerData.syntax)
    }.map(rs => mqttControllerData(rs)).list().apply().foreach(controllerData => {
      val newController = new mqttController(controllerData.masterid, controllerData.key, controllerData.name)
      controller += ((controllerData.masterid, newController))
    })

    withSQL {
      select.from(device as m)
    }.map(rs => device(rs)).list().apply().foreach(d => {
      d.switchtype match {
        case "mqtt" =>
          val mq = mqtt.syntax("mq")
          val mqttData = withSQL {
            select.from(mqtt as mq).where.eq(mq.id, d.id)
          }.map(rs => mqtt(rs)).single().apply().get

          var newDevice = new mqttSwitch(d.id, d.keepstate, d.name, d.controltype, mqttData.port)

          if (controller.contains(mqttData.masterid)) {
            newDevice.controller = controller(mqttData.masterid)
          } else {
            logger.error("""Could not find Controller with masterId: "%s" for switch: %s with ID: %s""", mqttData.masterid, d.name, d.id)
          }

          devices += ((d.id, newDevice))
          if (d.keepstate) {
            devices(d.id).on(d.state)
          }
        case "433Mhz" =>
          val m = mhz.syntax("m")
          val data = withSQL {
            select.from(mhz as m).where.eq(m.id, d.id)
          }.map(rs => mhz(rs)).single().apply().get
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
   * @param newDevice The device that should be added
   */
  def addDevice(newDevice: Switch): Unit = {
    val wrongclass = new IllegalArgumentException("""Can not add Switch ID:%s because switch of unknown type %s has no save definition""".format(newDevice.id, newDevice.getClass))
    withSQL {
      insertInto(device).values(newDevice.id, newDevice.name, newDevice.switchtype, if (newDevice.keepStatus) newDevice.status else 0, newDevice.keepStatus, newDevice.controlType)
    }.update.apply
    newDevice match {
      case device: mhzSwitch => withSQL {
        insertInto(mhz).values(device.id, device.systemCode, device.unitCode)
      }.update().apply()
        mhzId += ((device.systemCode + device.unitCode, device.id))
      case device: mqttSwitch => withSQL {
        insertInto(mqtt).values(device.id, device.masterId, device.pin())
      }.update().apply()
      case _ => throw wrongclass
    }
    devices = devices.concat(Map((newDevice.id, newDevice)))
    implicit val formats: Formats = Serialization.formats(NoTypeHints) + new switchSerializer
    websocket.broadcastMsg(("Command" -> "newDevice") ~ ("answer" -> JsonMethods.parse(write(newDevice))))
  }


  def updateDevice(oldid: String, pDevice: commandUpdateDevice): Unit = {
    logger.debug("update Start with new switch data: {}", pDevice)
    val wrongClass = new IllegalArgumentException("""Can not add Switch ID:%s because switch of unknown type %s has no save definition""".format(pDevice.newId, pDevice.getClass))
    if (pDevice.switchType == devices(oldid).switchtype) {
      devices(oldid).id = pDevice.newId
      devices(oldid).name = pDevice.name
      devices(oldid).keepStatus = pDevice.keepState
      devices(oldid).controlType = pDevice.controlType
    } else {
      devices(oldid) = Switch(pDevice)
    }

    logger.debug(s"devices({}) is now {}", oldid, devices(oldid))

    withSQL {
      update(device).set(
        device.column.id -> pDevice.newId,
        device.column.name -> pDevice.name,
        device.column.keepstate -> pDevice.keepState,
        device.column.switchtype -> pDevice.switchType,
        device.column.controltype -> pDevice.controlType
      ).where.eq(device.column.id, pDevice.newId)
    }.update.apply()
    if (oldid != pDevice.newId) {
      devices += (pDevice.newId -> devices(oldid))
      devices -= oldid
    }

    devices(pDevice.newId) match {
      case newDevice: mhzSwitch =>
        newDevice.systemCode = pDevice.systemCode
        newDevice.unitCode = pDevice.unitCode
        withSQL {
        update(mhz).set(
          mhz.column.id -> newDevice.id,
          mhz.column.systemcode -> newDevice.systemCode,
          mhz.column.unitcode -> newDevice.unitCode).where.eq(mhz.column.id, oldid)
      }.update().apply()
        mhzId(newDevice.systemCode + newDevice.unitCode) = newDevice.id

      case newDevice: mqttSwitch =>
        newDevice.changePinAndController(pDevice.pin,pDevice.masterId)
        withSQL {
          update(mqtt).set(
            mqtt.column.id -> newDevice.id,
            mqtt.column.masterid -> newDevice.masterId,
            mqtt.column.port -> newDevice.pin
          ).where.eq(mqtt.column.id, oldid)
        }.update().apply()
      case _ => throw wrongClass
    }
  }


  /**
   * Deletes the switch form the Database and the Devices Map
   *
   * @param id The Id of the Switch to delet
   */
  def deleteDevice(id: String): Unit = {
    withSQL {
      delete.from(device).where.eq(device.column.id, id)
    }.update.apply()
    withSQL {
      delete.from(mhz).where.eq(mhz.column.id, id)
    }.update().apply()
    withSQL {
      delete.from(mqtt).where.eq(mqtt.column.id, id)
    }.update().apply()
    devices -= id
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
      update(device).set(device.column.state -> state).where.eq(device.column.id, id)
    }.update().apply()
  }

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
   * closes the database connections
   */
  def shutdown: Unit = {
    session.close()
  }

  /**
   * Message class for devices Table
   *
   * @param id          ID of the Device in the Format [0-9a-Z] five character long
   * @param name        The name of the Device any String lenght in the Table 64 character
   * @param switchtype  A String identification of the Switch type max length 16 character
   * @param state       an foatingpoint representation of the State when keepState is true 4 decimalpoints long
   * @param keepstate   Boolean that indicates if the State should be restored at turn on
   * @param controltype either slider or button
   */
  case class device(id: String, name: String, switchtype: String, state: Float, keepstate: Boolean, controltype: String)

  /**
   * Message class for Mhz Table only needed when Switchtype is MQTT
   *
   * @param id         ID of the Device in the Format [0-9a-Z] five character long
   * @param systemcode Systemcode for the Mhzreciever
   * @param unitcode   Unitcode for the Mhzreciever
   */
  case class mhz(id: String, systemcode: String, unitcode: String)

  /**
   * Message class for user Table
   *
   * @param username the username
   * @param passhash the argon2id hash
   */
  case class user(username: String, passhash: String)

  case class mqtt(id: String, masterid: String, port: Int)

  case class mqttControllerData(masterid: String, name: String, key: Array[Byte])

  /**
   * Syntax support Object for devices table
   */
  object device extends SQLSyntaxSupport[device] {
    override val tableName = "devices"

    def apply(rs: WrappedResultSet) = new device(
      rs.string("id"),
      rs.string("name"),
      rs.string("switchtype"),
      rs.float("state"),
      rs.boolean("keepstate"),
      rs.string("controltype"))
  }

  /**
   * Syntax support Object for Mhz table
   */
  object mhz extends SQLSyntaxSupport[mhz] {
    override val tableName = "Mhz"

    def apply(rs: WrappedResultSet): mhz = new mhz(
      rs.string("id"),
      rs.string("systemcode"),
      rs.string("unitcode"))
  }

  /**
   * Syntax support Object for user table
   */
  object user extends SQLSyntaxSupport[user] {
    override val tableName = "User"

    def apply(rs: WrappedResultSet): user = new user(
      rs.string("username"),
      rs.string("passhash"))
  }

  object mqtt extends SQLSyntaxSupport[mqtt] {
    override val tableName = "mqtt"

    def apply(rs: WrappedResultSet): mqtt = new mqtt(
      rs.string("id"),
      rs.string("masterid"),
      rs.int("port"))
  }

  object mqttControllerData extends SQLSyntaxSupport[mqttControllerData] {
    override val tableName = "mqttController"

    def apply(rs: WrappedResultSet): mqttControllerData = new mqttControllerData(
      rs.string("masterid"),
      rs.string("name"),
      rs.bytes("key"))
  }

}
