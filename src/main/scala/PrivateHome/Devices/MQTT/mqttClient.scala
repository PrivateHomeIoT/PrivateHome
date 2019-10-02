package PrivateHome.Devices.MQTT

import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.eclipse.paho.client.mqttv3.{MqttClient, MqttMessage, _}

object mqttClient {

  private val brokerUrl = "tcp://localhost:429"
  //private val stat = "Home/switch/stat/#"
  //private val cmnd = "Home/switch/cmnd/#"
  private val home = "Home/#"
  private val persistence = new MemoryPersistence
  private val client = new MqttClient(brokerUrl, MqttClient.generateClientId, persistence)
  var lastMessage: String = ""
  var lastTopic: String = ""

  client.connect

  //client.subscribe(stat)
  //client.subscribe(cmnd)
  client.subscribe(home)

  val callback = new MqttCallback {
    override def messageArrived(topic: String, message: MqttMessage): Unit = {
      println("Receiving Data, Topic : %s, Message : %s".format(topic, message))
      lastMessage = message.toString
      lastTopic = topic
    }

    override def connectionLost(cause: Throwable): Unit = println(cause)

    override def deliveryComplete(token: IMqttDeliveryToken): Unit = {
    }
  }
  client.setCallback(callback)

  def publish(topic: String, message: String): Unit = {
    val msgTopic = client.getTopic(topic)
    val msg = new MqttMessage(message.getBytes("utf-8"))
    msgTopic.publish(msg)
  }
}
