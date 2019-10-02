package PrivateHome.Devices.MHz

import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListenerDigital}

class GpioPinListener extends GpioPinListenerDigital{
  override def handleGpioPinDigitalStateChangeEvent(event: GpioPinDigitalStateChangeEvent): Unit = ???

}
