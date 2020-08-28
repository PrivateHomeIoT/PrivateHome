package PrivateHome.Devices.MQTT

import PrivateHome.{data,settings}
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.eclipse.paho.client.mqttv3.{MqttClient, MqttMessage, _}

object mqttClient {

  private val brokerUrl = "tcp://" + settings.mqtt.url + ":" + settings.mqtt.port
  private val home = "Home/#"
  private val stat = "Home/stat/"
  private val persistence = new MemoryPersistence
  private val client = new MqttClient(brokerUrl, MqttClient.generateClientId, persistence)

  var lastMsg: String = "" //here you can get the last received message
  var lastID: String = "" //her you can get the last received id

  client.connect()
  client.subscribe(home)

  val callback: MqttCallback = new MqttCallback {

    /**
     * This method acts after recieving an MQTT-Message
     *
     * @param topic   is the topic in which the message was recieved. Mostly it contains the id, which you can see better in lastID.
     * @param message is the message which was recieved. You can also get it from lastMsg.
     */
    override def messageArrived(topic: String, message: MqttMessage): Unit = {
      println("Receiving Data, Topic : %s, Message : %s".format(topic, message))
      if(topic.charAt(topic.length-6) == '/' &&  topic.substring(0,10) == stat){
        lastID = ""
        lastMsg = ""
        lastID = topic.substring(topic.length - 5)
        lastMsg = message.toString
        if (lastMsg == "ON") data.devices(lastID).Status(1)
        else if (lastMsg == "OFF") data.devices(lastID).Status(0)
      }
      else println("invalid topic")
    }

    /**
     * This is the method, which acts if the connection is lost
     *
     * @param cause is the error, why the connection is lost.
     */
    override def connectionLost(cause: Throwable): Unit = println(cause)

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
  def publish(topic: String, message: String): Unit = {
    val msgTopic = client.getTopic(topic)
    val msg = new MqttMessage(message.getBytes("utf-8"))
    msgTopic.publish(msg)
  }
}
