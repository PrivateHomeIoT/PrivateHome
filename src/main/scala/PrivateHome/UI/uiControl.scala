package PrivateHome.UI

import PrivateHome.Devices.Switch
import PrivateHome.data
import PrivateHome.data.devices
import org.json4s.JsonAST
import org.json4s.JsonAST.{JField, JObject}
import org.json4s.JsonDSL._

object uiControl {
  def receiveCommand(command: Command): Any = {
    command match {
      case c: commandOn => devices(c.id).on(c.percent)
      case c: commandOff => devices(c.id).off()
      case _: commandGetDevices =>
        val devicesJson: List[JsonAST.JObject] = List()
        for (device <- devices) {
          devicesJson.concat(List(("id" -> device._2.id) ~ ("status" -> device._2.Status)))
        }
        JObject(JField("devices", devicesJson)) // because we don't use the "~" we must lift it to JSON that is why we use JObject(JField()) instead an simple "devices" -> devicesJSon.
      case c: commandAddDevice => data.addDevice(Switch(c)); JObject(JField("Success",true))

    }
  }
}
