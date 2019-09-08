package PrivateHome


import PrivateHome.Devices.MHz.{MHz_Connect, mhzCommand, mhzSwitch, queue}
import PrivateHome.Devices.Switch

import scala.util.Try

object main {
  def main(args: Array[String]): Unit = {
    val test = new editXML()
    //test.addObject()
    test.removeObject("object.getId()")
    /*
    println("Hello World")


    for (x <- args) println(x)
    println(args.length)

    //val Mhz = new MHz_Connect()
    var Command: Boolean = true
    var x: String = "11111"
    var y: String = "10000"


    if (args.length == 3) {
      x = args(0)
      y = args(1)
      Command = Try(args(2).toBoolean).getOrElse(false)
    }
    */
    /*
    val switch: mhzSwitch = new mhzSwitch("abcde",true,"11111","10000")
    println(switch.toXml())
    val test = Switch(data = <switch>
      <type>433MHz</type>
      <id>abcde</id>
      <keepStatus>true</keepStatus>
      <systemCode>11111</systemCode>
      <unitCode>10000</unitCode>
    </switch>)
    println(test.toXml)
    */

    /*
    val mhzThread = new Thread {
      override def run(): Unit = {
        Mhz.send()
      }
    }

    mhzThread.start()

     */
  }
}
