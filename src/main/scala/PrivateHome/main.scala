package PrivateHome

object main {
  def main(args: Array[String]): Unit = {
    println("Hello World")

    val Mhz = new MHz_Connect()

    val x:Array[Char] = Array('0','0','1','0','0')
    val y:Array[Char] = Array('0','0','1','0','0')

    Mhz.send(x,y,command = true)

  }
}
