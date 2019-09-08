package PrivateHome

import scala.xml._

class editXML {

  def addObject(): Unit = {

    val device = XML.load("src/main/scala/PrivateHome/devices.xml")
    val idXml = XML.load("src/main/scala/PrivateHome/id.xml")
    println(device)
    print(idXml)

    val name = "object.getName()"
    val id = "object.getId()"
    val room = "object.getRoom()"
    val typE = "object.getType()"
    //if(object.getType == MHz){
    //    val systemCode = object.getSystemCode()
    //    val unitCode = object.getUnitCode()
    //    }
    val systemCode = "object.getSystemCode()"
    val unitCode = "object.getUnitCode()"

    val id_prep = <id>{id}</id>
    val id_result = addANode(idXml, id_prep)
    XML.save("src/main/scala/PrivateHome/id.xml", id_result)

    val prep =
      <object id={id}>
        &#9; <name>
        {name}
      </name>
        &#9; <room>
        {room}
      </room>
        &#9; <type>
        {typE}
      </type>
        &#9; <systemCode>
        {systemCode}
      </systemCode>
        &#9; <unitCode>
        {unitCode}
      </unitCode>
      </object>
    val result = addANode(device, prep)
    XML.save("src/main/scala/PrivateHome/devices.xml", result)
  }

  def removeObject(id: String): Unit = {
    val devices = XML.load("src/main/scala/PrivateHome/devices.xml")
    val idXml = XML.load("src/main/scala/PrivateHome/id.xml")

    //val delID = """"""" + id + """""""
    val children = devices.child.foldLeft(NodeSeq.Empty)((acc, elem) => if((elem \ "@id").text == id) acc else acc ++ elem)
    val result = devices.copy(child = children)

    val id_prep = <id>{id}</id>
    val id_children = deleteNodes(idXml, (elem) => elem.text == id_prep.text)
    //val id_children: NodeSeq = idXml.child.foldLeft(NodeSeq.Empty)(op = (acc_id, elem_id) => if (acc_id.text == id.toString) acc_id else acc_id ++ elem_id)
    val id_result = idXml.copy(child = id_children)

    println(id_result)
    println(id_prep.text)
    //println(result)
    //println(delID)
    println(id)

    XML.save("src/main/scala/PrivateHome/id.xml", id_result)
    XML.save("src/main/scala/PrivateHome/devices.xml", result)
  }

  private def deleteNodes(n: Elem, f: (Node) => Boolean): NodeSeq = n.child.foldLeft(NodeSeq.Empty)((acc, elem) => if (f(elem)) acc else acc ++ elem)

  private def addANode(to: Node, newNode: Node): Node = to match {
    case Elem(prefix, label, attributes, scope, child@_*) => Elem(prefix, label, attributes, scope, child ++ newNode: _*)
    case _ => println(" could not find node ");
      to
  }
  /* def deleteNodesWithValue(n: Elem, value: String): Elem = {
    val children = deleteNodes(n, (elem) => elem.text == value)
    //val children = n.child.foldLeft(NodeSeq.Empty)((acc, elem) => if (acc.text == value) acc else acc ++ elem)
    n.copy(child = children)
  } */

  /*private def deleteNodesWithAttributeValue(n: Elem, value: String) = {
    val children = devices.child.foldLeft(NodeSeq.Empty)((acc, elem) => if((elem \ "@id").text == id) acc else acc ++ elem)
    devices.copy(child = children)
  }
  */
}
