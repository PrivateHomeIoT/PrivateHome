package PrivateHome.UI

import PrivateHome.data.devices
import org.json4s.native.JsonMethods._

object uiControl {
    def receiveCommand(command:Command): Unit = {
        command match {
            case c:commandOn => devices(c.id).on(c.percent)
            case c:commandOff => devices(c.id).off()
            case c:commandGetDevice =>
        }

    }
}
