package PrivateHome.UI
import PrivateHome.Devices.{Switch, switchSerializer}
import PrivateHome.data
import PrivateHome.data.devices
import org.json4s.JsonAST.{JField, JObject, JValue}
import org.json4s.JsonDSL._
import org.json4s.{Formats, NoTypeHints}
import org.json4s.jackson.Serialization.write
import org.json4s.jackson.{JsonMethods, Serialization}

object uiControl {

  implicit val formats: Formats = Serialization.formats(NoTypeHints) + new switchSerializer

  def receiveCommand(command: Command): Any = {
    command match {
      case c: commandOn => devices(c.id).on(c.percentFloat)
      case c: commandOff => devices(c.id).off()
      case _: commandGetDevices =>
        var devicesJson: List[JValue] = List()
        for (device <- data.devices) {

          devicesJson = devicesJson.concat(List(JsonMethods.parse(write(device._2))))
        }
        JObject(JField("devices", devicesJson)) // because we don't use the "~" we must lift it to JSON that is why we use JObject(JField()) instead an simple "devices" -> devicesJSon.
      case c: commandAddDevice => data.addDevice(Switch(c)); JObject(JField("Success", true))

    }
  }
}
