package PrivateHome.Devices.MHz

import com.pi4j.io.gpio._

import scala.collection.mutable

class mhzConnect(Repeat: Int = 10, pulseLength: Long = 350,pIn: Int = 25, pOu: Int = 28) {


  /**
   * Waits for a given time in microseconds busy on one Core
   * @param micros Time the Process should wait
   */
  def busyWaitMicro(micros: Long): Unit = {
    val waitUntil: Long = System.nanoTime() + (micros * 1_000)
    while (waitUntil > System.nanoTime()) {}
  }

  private val queue = new mutable.Queue[mhzCommand]

  private var sending = false

  private val gpio: GpioController = GpioFactory.getInstance()

  private val output:GpioPinDigitalOutput = gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(pOu), "433_Out", PinState.LOW)

  private val input:GpioPinDigitalInput = gpio.provisionDigitalInputPin(RaspiPin.getPinByAddress(pIn), "433_Input")


  input.addListener(new GpioPinListener)

  /**
   * Adds Command to the queue and triggers sending
   * @param pCommand The Command to send
   */
  def send(pCommand: mhzCommand): Unit = {
    queue.enqueue(pCommand)
    if (!sending) sendCommand()


  }

  /**
   * Sends all Commands in the Queue
   */
  private def sendCommand(): Unit = {

    sending = true

    while (queue.nonEmpty) {

      val queuedCommand = queue.dequeue()

      if (!((queuedCommand.systemCode.length == 5) && (queuedCommand.unitCode.length == 5))) {
        throw new IllegalArgumentException
      }

      var code: Long = 0
      var length: Int = 0

      for (c: Char <- queuedCommand.systemCode) {
        code <<= 2
        if (c == '0') code |= 0x01
        length += 2
      }

      for (c: Char <- queuedCommand.unitCode) {
        code <<= 2
        if (c == '0') code |= 0x01
        length += 2
      }

      code <<= 2
      if (!queuedCommand.command) code |= 0x01
      length += 2
      code <<= 2
      if (queuedCommand.command) code |= 0x01
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
    sending = false
  }




}

object sendMhz {
  val mhz = new mhzConnect()

  def apply(pCommand: mhzCommand):Unit = mhz.send(pCommand)
}

