package PrivateHome

import PrivateHome.Devices.MHz.{MHz_Connect, mhzCommand, queue}

import scala.util.Try

object main {
  def main(args: Array[String]): Unit = {
    println("Hello World")


    for (x <- args) println(x)
    println(args.length)

    val Mhz = new MHz_Connect()
    var Command: Boolean = true
    var x: String = "11111"
    var y: String = "10000"


    if (args.length == 3) {
      x = args(0)
      y = args(1)
      Command = Try(args(2).toBoolean).getOrElse(false)
    }

    queue.queue.enqueue(mhzCommand(x, y, Command))


    val mhzThread = new Thread {
      override def run(): Unit = {
        Mhz.send()
      }
    }

    mhzThread.start()


  }
}
