package PrivateHome.UI

import PrivateHome.data.idTest

class Command()

case class commandOn(id: String, private var percent: String) extends Command {
    idTest(id)
    var percentFloat = percent.toFloat
    if (0 > percentFloat || percentFloat > 1) throw new IllegalArgumentException("percent has to be between 0 and 1")
}

case class commandOff(id: String) extends Command {
    idTest(id)
}

case class commandGetDevices() extends Command

case class commandSettingsMain(setting: String, value: AnyVal) extends Command

case class commandSettingsDevice(id: String, setting: String, value: AnyVal) extends Command {
    idTest(id)

}

case class commandAddDevice(id: String, switchType: String,name: String,systemCode: String="",unitCode: String="", controlType: String, keepState: Boolean) extends Command {
    idTest(id,create = true)
    switchType match {
        case "mqtt" =>
        case "433Mhz" =>
            if (systemCode.length != 5) throw new IllegalArgumentException("""Length of systemCode is not 5""")
            if (!systemCode.matches("[01]{5}")) throw new IllegalArgumentException("""systemCode Contains not Allowed Characters""")
            if (systemCode.length != 5) throw new IllegalArgumentException("""Length of systemCode is not 5""")
            if (!systemCode.matches("[01]{5}")) throw new IllegalArgumentException("""systemCode Contains not Allowed Characters""")
    }
}

