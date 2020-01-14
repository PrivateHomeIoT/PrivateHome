package PrivateHome.Devices.MHz

object Protocol {
  val pulseLength = 350
  val sync = Signal(1,31)
  val zero = Signal(1,3)
  val one = Signal(3,1)
}

case class Signal(high:Int,low:Int)

object codec {
  val code: Map[Char,Int] = Map('0'->0x01,'1' -> 0x00)

  def command(comm: Boolean): Int = {
    if (comm) 0x0001
    else 0x0100
  }
}

