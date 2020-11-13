package PrivateHome.Devices

import PrivateHome.Devices.MHz.mhzSwitch
import PrivateHome.Devices.MQTT.mqttSwitch
import PrivateHome.UI.commandAddDevice
import PrivateHome.{data, editXML}
import PrivateHome.data.idTest
import PrivateHome.UI.Websocket.websocket
import org.json4s.JsonAST.JObject
import org.json4s.{CustomSerializer, JsonAST}
import org.json4s.JsonDSL._

import scala.xml._

/**
 * The general Switch class
 *
 * @param setupID         an unique Base64 ID
 * @param setupKeepStatus toggles if the Switch should save State over program restart (failure)
 */

abstract class Switch(private val setupID: String, setupKeepStatus: Boolean, var name:String, private var _controlType:String) {
    idTest(setupID)
    if (_controlType != "button"&&_controlType != "slider") throw new IllegalArgumentException("controlType isn't button/slider")

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
        websocket.broadcastMsg(("Command" -> "statusChange") ~ ("answer" -> (("id" -> id) ~ ("status" -> state) ~ ("type" -> switchtype))))
    }

    def id: String = setupID
    
    def controlType:String = _controlType
    
    def controlType(newType:String):Unit = {
        if (newType != "button"||newType != "slider") throw new IllegalArgumentException("controlType isn't Button/slider")
        _controlType = newType
    }

    def switchtype: String

    def Status: Float = _status

    def toXml: Node

    def keepStatus: Boolean = setupKeepStatus



    def serializer: JsonAST.JObject = {
        ("id" -> id) ~ ("keepState"->keepStatus) ~ ("name"->name) ~ ("switchType"->switchtype) ~ ("controlType"->controlType) ~ ("status"-> Status)
    }

}

object Switch {
    def apply(data: Node): Switch = {

        val switchType = (data \ "type").head.text
        println(switchType)
        switchType match {
            case "433MHz" => val systemCode = (data \ "systemCode").head.text
                val unitCode = (data \ "unitCode").head.text
                val name = (data \ "name").head.text
                val KeepStatus = (data \ "keepStatus").head.text.toBoolean
                val ID = (data \ "@id").text
                mhzSwitch(ID, KeepStatus,name, systemCode, unitCode);
            case "MQTT" => val id = (data \ "@id").text
                val KeepStatus = (data \ "keepStatus").head.text.toBoolean
                val name = (data \ "name").head.text
                mqttSwitch(id, KeepStatus,name,"button")
            case _ => throw new IllegalArgumentException("Wrong Switch Type")
        }
    }

    def apply(data: commandAddDevice): Switch = {
        data.switchType match {
            case "433Mhz" => mhzSwitch(data.id,data.keepState,data.name,data.systemCode,data.unitCode)
            case "mqtt" => mqttSwitch(data.id,data.keepState,data.name,data.controlType)
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

class switchSerializer extends CustomSerializer[Switch](format => ({
  case jsonObj: JObject => mqttSwitch("",false,"This Switch should never be used","")
},{
  case switch:mqttSwitch => switch.serializer
  case switch: mhzSwitch => switch.serializer ~ ("systemCode"->switch.systemCode) ~("unitCode"->switch.unitCode)
}))