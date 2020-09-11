package PrivateHome.Devices.MQTT

import PrivateHome.Devices.Switch
import org.json4s.JsonDSL._
import org.json4s.CustomSerializer
import org.json4s.JsonAST.JObject

import scala.xml.Node

case class mqttSwitch(ID: String, setupKeepStatus: Boolean, _name: String, setupControlType:String) extends Switch(ID, setupKeepStatus, _name,setupControlType) {

  /**
   * This method turns on the MQTT-Device.
   */
  def on(percent: Float): Unit = mqttClient.publish("Home/switch/cmnd/" + id, percent.toString)

  /**
   * This method turns off the MQTT-Device.
   */
  def off(): Unit = mqttClient.publish("Home/switch/cmnd/" + id, "OFF")

  /**
   *  This method generates from the attributes of this class an XML which you can save with editXML.scala
   * @return It returns a Node which you can use in editXML.scala
   */
  override def toXml: Node = <switch id={ID}>
    <type>MQTT</type>
    <id>{ID}</id>
    <keepStatus>{keepStatus}</keepStatus>
  </switch>

  def switchtype: String = "MQTT"
}
