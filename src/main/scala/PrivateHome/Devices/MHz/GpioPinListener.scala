package PrivateHome.Devices.MHz

import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListenerDigital}
import scala.math

class GpioPinListener extends GpioPinListenerDigital{
  private val nSeperationlimit = 4600
  private val nRecieveTolerence = 60
  private val rcSwitchMaxChanges = 67
  private val timings:Array[Int] = new Array[Int](rcSwitchMaxChanges)
  private var changeCount = 0
  private var lastTime:Long = 0
  private var repeatCount = 0

  private def diffAbs(a:Int, b:Int): Int = math.abs(a-b)

  private def receiveProtocol(pChangeCount:Int): Unit = {
    println("Recieved Something")
  }



  override def handleGpioPinDigitalStateChangeEvent(event: GpioPinDigitalStateChangeEvent): Unit = {
    val time = System.nanoTime()/1000
    val signalDuration:Int = (time-lastTime).toInt

    if (signalDuration > nSeperationlimit) {
      // A long stretch without signal level change occurred. This could
      // be the gap between two transmission.
      if (diffAbs(signalDuration,timings(0)) < 200) {
        // This long signal is close in length to the long signal which
        // started the previously recorded timings; this suggests that
        // it may indeed by a a gap between two transmissions (we assume
        // here that a sender will send the signal multiple times,
        // with roughly the same gap between them).
        repeatCount += 1
        if (repeatCount == 2) {
          receiveProtocol(repeatCount)
          repeatCount = 0
        }

      }
      changeCount = 0
    }

    if (changeCount >= rcSwitchMaxChanges) {
      changeCount = 0
      repeatCount = 0
    }
    timings(changeCount) = signalDuration
    changeCount += 1
    lastTime = time
  }

}