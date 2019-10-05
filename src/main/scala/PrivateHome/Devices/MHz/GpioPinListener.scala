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

  private def receiveProtocol(pChangeCount:Int): Boolean = {
    if (pChangeCount > 7) {
      var code: Long = 0
      val delay = timings(0) / Protocol.sync.low
      val delayTolerance = delay * nRecieveTolerence / 100

      for (i <- 1 until pChangeCount-1 by 2) {
        code <<= 1
        if (diffAbs(timings(i), delay * Protocol.zero.high) < delayTolerance &&
          diffAbs(timings(i + 1), delay * Protocol.zero.low) < delayTolerance) {
          //Zero
        } else if (diffAbs(timings(i), delay * Protocol.one.high) < delayTolerance &&
          diffAbs(timings(i + 1), delay * Protocol.one.low) < delayTolerance) {
          //One
          code |= 1
        } else {
          //Failed

        }
      }
      interpretCode(code,(pChangeCount-1)/2)
    }
    else false


  }

  private def interpretCode(pcode:Long,bitCount:Int): Boolean ={
    var code = pcode
    var commandCode = ""


    val command = (code & 15) == codec.command(true)
    code >>= 4
    for (i <- 0 until bitCount-4 by 2){

      if ((code & 3) == codec.code('0')) {
        commandCode += "0"
        code >>= 2
      }
      else if ((code & 3) == codec.code('1')) {
        commandCode += "1"
        code >>= 2
      }
      else {
        println("error" + (code & 3))
        return false

      }


    }

    commandCode = commandCode.reverse

    val systemCode = commandCode.substring(0,5)
    val unitCode = commandCode.substring(5)
    //ToDo: needs to call a change state at the switch

    println(s"""An: $command; SystemCode: $systemCode UnitCode: $unitCode""")

    true
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
          receiveProtocol(changeCount)
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
