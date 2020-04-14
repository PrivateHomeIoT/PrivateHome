package PrivateHome.UI

import PrivateHome.data.idTest

class Command()

case class commandOn(id: String, percent: Float) extends Command {
    idTest(id)
    if (0 > percent || percent > 1) throw new IllegalArgumentException("percent has to be between 0 and 1")
}

case class commandOff(id: String) extends Command {
    idTest(id)
}

case class commandGetDevices() extends Command

case class commandSettingsMain(setting: String, value: AnyVal) extends Command

case class commandSettingsDevice(id: String, setting: String, value: AnyVal) extends Command {
    idTest(id)

}

