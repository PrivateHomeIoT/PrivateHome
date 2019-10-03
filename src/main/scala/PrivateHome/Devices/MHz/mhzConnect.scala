package PrivateHome.Devices.MHz

import com.pi4j.io.gpio._
import PrivateHome.Devices.MHz.Protocol

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

      val commandCode: String = queuedCommand.systemCode + queuedCommand.unitCode

      for (c: Char <- commandCode) {
        code <<= 2
        codec.code(c)
        length += 2
      }

      code <<= 4
      codec.command(queuedCommand.command)
      length += 4


      for (count <- 0 until Repeat) {
        for (i <- length - 1 to 0 by -1) {
          if (((code >> i) & 0x1) == 1) {
            output.high()
            busyWaitMicro(Protocol.one.high*Protocol.pulseLenght)
            output.low()
            busyWaitMicro(Protocol.one.low*Protocol.pulseLenght)

          } else {
            output.high()
            busyWaitMicro(Protocol.zero.high*Protocol.pulseLenght)
            output.low()
            busyWaitMicro(Protocol.zero.low*Protocol.pulseLenght)
          }
        }

        output.high()
        busyWaitMicro(Protocol.sync.high*Protocol.pulseLenght)
        output.low()
        busyWaitMicro(Protocol.sync.low*Protocol.pulseLenght)

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

