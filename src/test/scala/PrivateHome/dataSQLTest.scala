package PrivateHome

import org.scalatest.FunSuite

class dataSQLTest extends FunSuite {

  test("getDevices") {
    dataSQL.create()
    println (dataSQL.getDevices)
  }

}
