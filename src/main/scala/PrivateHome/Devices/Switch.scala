package PrivateHome.Devices

import PrivateHome.Devices.MHz.mhzSwitch
import PrivateHome.Devices.MQTT.mqttSwitch
import PrivateHome.{data, editXML}
import PrivateHome.data.idTest

import scala.xml._

/**
 * The general Switch class
 *
 * @param setupID         an unique Base64 ID
 * @param setupKeepStatus toggles if the Switch should save State over program restart (failure)
 */

abstract class Switch(private val setupID: String, setupKeepStatus: Boolean, name:String) {
    idTest(setupID)

    private var _status:Float = 0

    def on(percent: Float): Unit

    def off(): Unit

    /**
     * Sets the Status of the Switch only changes State after receiving a Confirmation
     *
     * @param state The State the Switch should change to
     */
    def Status(state: Float): Unit = {
        _status = state
        if (setupKeepStatus) data.saveStatus(id,state)
    }

    def id: String = setupID

    def switchtype: String

    def Status: Float = _status

    def toXml: Node

    def keepStatus: Boolean = setupKeepStatus

}

object Switch {
    def apply(data: Node): Switch = {
        val switchType = (data \ "type").head.text
        println(switchType)
        switchType match {
            case "433MHz" => val systemCode = (data \ "systemCode").head.text
                val unitCode = (data \ "unitCode").head.text
                val KeepStatus = (data \ "keepStatus").head.text.toBoolean
                val ID = (data \ "@id").text
                mhzSwitch(ID, KeepStatus, systemCode, unitCode);
            case "MQTT" => val id = (data \ "@id").text
                val KeepStatus = (data \ "keepStatus").head.text.toBoolean
                mqttSwitch(id, KeepStatus)
            case _ => throw new IllegalArgumentException("Wrong Switch Type")
        }
    }

    def on(percent: Float, id: String): Unit = {
        val tempSwitch = data.devices(id)
        tempSwitch.on(percent)
    }

    def off(id: String): Unit = {
        val tempSwitch = data.devices(id)
        tempSwitch.off()
    }
}