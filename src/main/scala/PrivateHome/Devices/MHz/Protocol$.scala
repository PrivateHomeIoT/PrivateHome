package PrivateHome.Devices.MHz

object Protocol {
  val pulseLenght = 350
  val sync = Signal(1,31)
  val zero = Signal(1,3)
  val one = Signal(3,1)
}

case class Signal(high:Int,low:Int) {

}
