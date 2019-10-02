package PrivateHome.Devices.MQTT

import PrivateHome.Devices.Switch

import scala.xml.Node

case class mqttSwitch(ID: String, keepStatus: Boolean) extends Switch(ID, keepStatus) {

  def on(): Unit = mqttClient.publish("Home/switch/cmnd/" + id, "ON")

  def off(): Unit = mqttClient.publish("Home/switch/cmnd/" + id, "OFF")

  override def toXml: Node = <switch id={ID}>
    <type>MQTT</type>
    <id>
      {ID}
    </id>
    <keepStatus>
      {keepStatus}
    </keepStatus>
  </switch>
}
