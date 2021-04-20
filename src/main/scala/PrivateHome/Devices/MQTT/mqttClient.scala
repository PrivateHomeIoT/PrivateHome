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

import PrivateHome.{data, settings}
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.eclipse.paho.client.mqttv3.{MqttClient, MqttMessage, _}
import org.slf4j.LoggerFactory

import java.nio.charset.Charset

object mqttClient {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val brokerUrl = "tcp://" + settings.mqtt.url + ":" + settings.mqtt.port
  private val home = "home/"
  private val persistence = new MemoryPersistence
  private var clientID = MqttClient.generateClientId
  clientID = clientID.substring(0, Math.min(23, clientID.length))
  private val client = new MqttClient(brokerUrl, clientID, persistence)

  var lastMsg: String = "" //here you can get the last received message
  var lastID: String = "" //her you can get the last received id

  client.connect()
  client.subscribe(stat.topicString + "#")
  client.subscribe(setupRequest.topicString+ "#")

  val callback: MqttCallback = new MqttCallback {

    /**
     * This method acts after recieving an MQTT-Message
     *
     * @param topic   is the topic in which the message was recieved. Mostly it contains the id, which you can see better in lastID.
     * @param message is the message which was recieved. You can also get it from lastMsg.
     */
    override def messageArrived(topic: String, message: MqttMessage): Unit = {

      try {
      val payloadString = new String(message.getPayload, Charset.forName("ASCII"))
      val payload: Array[Byte] = payloadString.split(",").map(_.toInt).map(_.toByte)
      logger.trace("Received message in Topic {}: length: {} {}", topic, payload.length, payload.map("%02X" format _))

        if (topic.startsWith(stat.topicString)) {
          val randomCode = topic.substring(topic.length - 10)
          val controller = data.getControllerRandomCode(randomCode)
          controller.receiveStatuschange(payload)
        }
        //else if (topic.startsWith(publishTopic.cmnd)) logger.trace("Received own command {} for \"{}\"", topic.substring(topic.length-5),message.toString)
        else if (topic.startsWith(setupRequest.topicString)) {
          val masterID = topic.substring(topic.length - 5)
          if (data.masterIdExists(masterID)) {
            val controller = data.getControllerMasterId(masterID)
            if (controller.checkSetup(payload)) {
              logger.debug("Doing Setup for {}", masterID)
              controller.setupClient()
            } else {
              logger.debug("A setup for {} was requested but not granted because the message wasn't correctly encrypted.", masterID)
            }
          } else logger.debug("A setup for {} was requested but not granted because the masterID was unknown.", masterID)
        }
        else logger.warn("Received message in unknown Topic: {} and Message: {}", topic, message.toString)
      } catch {
        case e:Throwable => logger.warn("Error in MQTT message handler",e)
      }
    }

    /**
     * This is the method, which acts if the connection is lost
     *
     * @param cause is the error, why the connection is lost.
     */
    override def connectionLost(cause: Throwable): Unit = {
      logger.warn("Lost mqtt connection!", cause)
    }

    /**
     * I think, it's the method, which acts after sending a MQTT-Message
     *
     * @param token I don't know what it is ... It's for internal usage.
     */
    override def deliveryComplete(token: IMqttDeliveryToken): Unit = {
    }
  }
  client.setCallback(callback)

  /**
   * This method publishes a message in a topic.
   *
   * @param topic   Is the topic in which you want to publish your message.
   * @param message Is the message you want to publish.
   */
  @deprecated("This is deprecated because you should only use publish(topic: topic,message: Array[Byte]")
  def publish(topic: String, message: String): Unit = {


    val msg = new MqttMessage(message.getBytes("utf-8"))
    val topicswit = client.getTopic(topic)
    topicswit.publish(msg)
  }

  def publish(topic: publishTopic,message: Array[Byte]): Unit = {
    val messageConverted = message.map(_ & 0xFF).mkString(",")
    val msg = new MqttMessage(messageConverted.getBytes("ASCII"))
    val topicswit = client.getTopic(topic.topicString)
    topicswit.publish(msg)
    logger.debug("Send message in topic {} length: {}", topic.topicString, message.length)
  }

  def shutdown: Unit = {
    client.disconnect()
    client.close()
  }


  class setup(code: String) extends publishTopic(home + "setup/" + code)

  class cmnd(code: String) extends publishTopic(home + "switch/cmnd/" + code)

  case class topic(topicString: String)

  class publishTopic(topic: String) extends topic(topic)

  class subscribeTopic(topic: String) extends topic(topic)

  object stat extends subscribeTopic(home + "stat/")

  object setupRequest extends subscribeTopic(home + "setupRequest/")

}
