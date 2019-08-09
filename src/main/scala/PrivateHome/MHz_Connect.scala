package PrivateHome

import com.pi4j.io.gpio._


class MHz_Connect(Repeat: Int = 15, pulseLength: Long = 350) {

  val gpio: GpioController = GpioFactory.getInstance()

  val output: GpioPinDigitalOutput = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_20, "433_Out", PinState.LOW)


  def send(systemcode: Array[Char], unitCode: Array[Char], command: Boolean): Unit = {
    if (!((systemcode.length == 5) && (unitCode.length == 5))) {
      throw new IllegalArgumentException
    }
    var sendCode = new Array[Char](12)
    var nReturnPos: Int = 0

    for (c: Char <- systemcode) {
      sendCode(nReturnPos) = if (c == '0') 'F' else '0'
      nReturnPos += 1
    }

    for (c: Char <- unitCode) {
      sendCode(nReturnPos) = if (c == '0') 'F' else '0'
      nReturnPos += 1
    }

    sendCode(10) = if (command) '0' else 'F'
    sendCode(11) = if (command) 'F' else '0'


    var code: Long = 0
    var length: Int = 0


    for (charCode <- sendCode) {
      code <<= 2
      if (charCode == 'F') code |= 0x01
      length += 2
    }


    for (count <- 0 until Repeat) {
      for (i <- length - 1 to 0 by -1) {
        if (((code >> i) & 0x1) == 1) {
          output.pulse(3 * pulseLength)
          wait(pulseLength)
        } else {
          output.pulse(pulseLength)
          wait(3 * pulseLength)
        }
      }

      output.pulse(pulseLength)
      wait(31 * pulseLength)

      println()
    }

  }


}


