package PrivateHome

import com.pi4j.io.gpio._


class MHz_Connect(Repeat: Int = 10, pulseLength: Long = 350) {

  def busyWaitMicro(micros: Long): Unit = {
    val waitUntil: Long = System.nanoTime() + (micros * 1_000)
    while (waitUntil > System.nanoTime()) {}
  }

  val gpio: GpioController = GpioFactory.getInstance()

  val output: GpioPinDigitalOutput = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_28, "433_Out", PinState.LOW)


  def send(systemcode: String, unitCode: String, command: Boolean): Unit = {
    if (!((systemcode.length == 5) && (unitCode.length == 5))) {
      throw new IllegalArgumentException
    }

    var code: Long = 0
    var length: Int = 0

    for (c: Char <- systemcode) {
      code <<= 2
      if (c == '0') code |= 0x01
      length += 2
    }

    for (c: Char <- unitCode) {
      code <<= 2
      if (c == '0') code |= 0x01
      length += 2
    }

    code <<= 2
    if (!command) code |= 0x01
    length += 2
    code <<= 2
    if (command) code |= 0x01
    length += 2




    for (count <- 0 until Repeat) {
      for (i <- length - 1 to 0 by -1) {
        if (((code >> i) & 0x1) == 1) {
          output.high()
          busyWaitMicro(3 * pulseLength)
          output.low()
          busyWaitMicro(pulseLength)

        } else {
          output.high()
          busyWaitMicro(pulseLength)
          output.low()
          busyWaitMicro(3 * pulseLength)
        }
      }

      output.high()
      busyWaitMicro(pulseLength)
      output.low()
      busyWaitMicro(31 * pulseLength)

      println()
    }

  }


}


