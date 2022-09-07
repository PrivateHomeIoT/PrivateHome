package PrivateHome.UI.Websocket


import PrivateHome.UI.GUI.gui.websocketRoute
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import org.json4s.{JInt, JObject, JValue}
import org.scalatest.funsuite.AnyFunSuite


class websocketTest extends AnyFunSuite with ScalatestRouteTest {

  test("testBroadcastMsg") {

    val wsClient = WSProbe()
    val wsClient2 = WSProbe()

    WS("/ws", wsClient.flow) ~> websocketRoute ~> check {
      WS("/ws", wsClient2.flow) ~> websocketRoute ~> check {
        assert(isWebSocketUpgrade)

        websocket.broadcastMsg("String Test")
        wsClient.expectMessage("String Test")
        wsClient2.expectMessage("String Test")

        websocket.broadcastMsg(JObject("Test" -> JInt(123)))
        wsClient.expectMessage("""{"Test":123}""")
      }
    }
  }
}
