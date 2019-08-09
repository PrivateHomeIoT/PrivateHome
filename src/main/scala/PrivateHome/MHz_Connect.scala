package PrivateHome

import com.pi4j.io.gpio._


class MHz_Connect(Repeat: Int = 15, pulseLength: Long = 350) {

  def busyWait(micros:Long): Unit = {
    val waitUntil:Long = System.nanoTime() + (micros * 1_000)
    while (waitUntil > System.nanoTime()) {}
  }

  val gpio: GpioController = GpioFactory.getInstance()

  val output: GpioPinDigitalOutput = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_28, "433_Out", PinState.LOW)


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
          output.high()
          busyWait(3*pulseLength)
          output.low()
          busyWait(pulseLength)

        } else {
          output.high()
          busyWait(pulseLength)
          output.low()
          busyWait(3*pulseLength)
        }
      }

      output.high()
      busyWait(pulseLength)
      output.low()
      busyWait(31*pulseLength)

      println()
    }

  }


}


