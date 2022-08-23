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

package PrivateHome.Devices.MQTT

import PrivateHome.Devices.MQTT.mqttClient.{cmd, setup}
import PrivateHome.{data, settings}
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.native.JsonMethods._
import org.slf4j.LoggerFactory

import java.io.{File, FileWriter}
import java.nio.charset.Charset
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}

class mqttController(val masterID: String, _key: Array[Byte], val name: String = "") {
  private val logger = LoggerFactory.getLogger(this.getClass)

  implicit val formats: DefaultFormats.type = DefaultFormats
  val cipher = Cipher.getInstance("AES/CBC/NoPadding")
  private val key = new SecretKeySpec(_key, "AES")

  private [PrivateHome] def keyArray: Array[Byte] = _key

  private val pins: Array[mqttSwitch] = new Array[mqttSwitch](64)
  private var _randomCode: String = ""

  def randomCode: String = _randomCode

  def addSwitch(pin: Int, pSwitch: mqttSwitch): Boolean = {
    pins(pin) match {
      case null =>
        pins(pin) = pSwitch
        setupClient()
        true
      case old: mqttSwitch =>
        false
    }
  }

  def deleteSwitch(pin: Int): Unit = {
    pins(pin) = null
    setupClient()
  }

  def changePin(oldPin: Int, newPin: Int): Boolean = {
    if (pins(newPin) == null) {
      pins(newPin) = pins(oldPin)
      pins(oldPin) = null
      setupClient()
      true
    } else false
  }

  def setupClient(): Unit = {
    _randomCode = data.controllerNewRandom(this)
    var output: List[JObject] = List()
    for (i: Int <- 0 to 63) {
      if (pins(i) != null) {
        val switch = pins(i)

        output = output.concat(List(("pin" -> i) ~ ("value" -> (switch.status * 1023).toInt)))
        logger.debug("Added a switch to output")
      }
    }

    // TODO: Support Inputs
    val message = ("randomCode" -> _randomCode) ~ ("outputs" -> JArray(output)) ~ ("inputs" -> JArray(List()))

    mqttClient.publish(new setup(masterID), encryptMessage(compact(render(message))))
  }

  private def encryptMessage(message: String): Array[Byte] = {
    encryptMessage(message.getBytes("ASCII"))
  }

  private def encryptMessage(message: Array[Byte]): Array[Byte] = {
    val IV = new Array[Byte](16)
    new SecureRandom().nextBytes(IV)
    val paddedMessage = new Array[Byte]((message.length/16+1)*16)
    for (i <- message.indices) {
      paddedMessage(i) = message(i)
    }
    val padding: Byte = (16 - (message.length % 16)).toByte
    for (i <- message.length until paddedMessage.length)
      paddedMessage(i) = padding
    cipher.init(Cipher.ENCRYPT_MODE, key, new SecureRandom())
    cipher.getIV ++ cipher.doFinal(paddedMessage)
  }

  def receiveStatuschange(message: Array[Byte]): Unit = {
    val messageString = decrypt(message)
    logger.debug("Trying to parse {}", messageString)
    val messageJson = parse(messageString)
    logger.debug("got parsed to {}",messageJson)
    val pin = (messageJson \ "pin").extract[Int]
    val value: Float = (messageJson \ "value").extract[String].toFloat / 1023
    logger.debug("Setting pin {} to {}", pin, value)
    pins(pin).status = value
  }

  def decrypt(message: Array[Byte]): String = {
    val iv = message.slice(0, 16)
    cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv))
    var messageDecrypted = cipher.doFinal(message.slice(16, message.length))
    logger.debug(messageDecrypted.map("%02X" format _).mkString("Message: ",",",""))
    messageDecrypted = messageDecrypted.slice(0,messageDecrypted.length-messageDecrypted.last)
    new String(messageDecrypted, "ASCII")
  }

  def checkSetup(message: Array[Byte]): Boolean = {
    val messageString = decrypt(message)
    val valid = messageString.equals(masterID)
    if (!valid) logger.info("Setup requested for {} with message {}", masterID, messageString)
    valid
  }

  def sendCommand(pin: Int, value: Float): Unit = {
    if (!(value >= 0 && value <= 1)) throw new IllegalArgumentException("Value should be between 0 and 1")
    sendCommand(pin, (value * 1023).toInt)
  }

  def sendCommand(pin: Int, value: Int): Unit = {
    if (!(value >= 0 && value < 1024)) throw new IllegalArgumentException("Value should be between 0 and 1023")
    mqttClient.publish(new cmd(randomCode), encryptMessage(compact(render(("pin" -> pin) ~ ("value" -> value)))))
    logger.debug("Send message pin: {} value: {}", pin, value)
  }

  def programController(path: String, ssid: String, wifiPass: String ): Unit = {
    val file = new FileWriter(new File(path),Charset.forName("US-ASCII"))
    if (!ssid.isBlank)
    file.write("setSSID\n%s\n".format(ssid))
    if (!wifiPass.isBlank)
    file.write("setPW\n%s\n".format(wifiPass))
    file.write("setServer\n%s\nsetKey\n%s\nsetID\n%s\n".format(settings.mqtt.url, _key.map(_ & 0xFF).mkString(","), masterID))
    file.flush()
    file.close()
  }
}