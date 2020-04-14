package PrivateHome.UI

import PrivateHome.data.devices

object uiControl {
    def receiveCommand(command:Command): Unit = {
        command match {
            case c:commandOn => devices(c.id).on(c.percent)
            case c:commandOff => devices(c.id).off()
            case c:commandGetDevices =>
        }

    }
}
