package PrivateHome

import PrivateHome.UI._
import org.scalatest.FunSuite

class websocketTest extends FunSuite{
//  test("get Devices"){
//    print(uiControl.receiveCommand(commandGetDevices()))
//  }
//  
//  test("listen to html"){
//    while (true) {
//      Websocket.websocket.listen("")
//    }
//  }
//  test("adddevice"){
//    uiControl.receiveCommand(commandAddDevice.apply("idhw2", "mqtt", "test1","", "", "switch", false))
//  }
  
  test("activate ws") {
    PrivateHome.UI.GUI.gui
  }
  //PrivateHome.UI.GUI.gui
}
