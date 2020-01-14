package PrivateHome

import PrivateHome.Devices.Switch

import scala.xml._

object data {

  /**
   * This variable returns a List containing all ids.
   */
  val IDs: List[String] = {
    val xml = XML.load("src/main/scala/PrivateHome/id.xml")
    val prep: NodeSeq = xml \\ "id"
    val result = for (i <- 0 until prep.length) yield prep(i).text
    result.toList
  }

  /**
   * This variable returns a map which contains the id as the key and the Switch-object for this id.
   */
  val devices: Map[String, Switch] = {
    val xml: Elem = XML.load("src/main/scala/PrivateHome/devices.xml")
    val prep: NodeSeq = xml \ "switch"
    val data: Seq[(String, Switch)] = for (i <- prep) yield (i \ "@id").text -> Switch(i)
    data.toMap
  }

  /**
   * This variable returns an map, which contains the MHz-Code(systemCode+unitCode) as the key and the id for this code.
   */
  val mhzID: Map[String, String] = {
    val xml: Elem = XML.load("src/main/scala/PrivateHome/devices.xml")
    val prep: NodeSeq = xml \ "switch"
    val data: Seq[(String, String)] = for (i <- prep) yield ((i \ "systemCode").text + (i \ "unitCode").text) -> (i \ "@id").text
    data.toMap
  }

  val settings: Map[String, Int] = Map(("port", 2888), ("ip", 565))

}
