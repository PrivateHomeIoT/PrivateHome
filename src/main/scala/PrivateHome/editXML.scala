package PrivateHome

import scala.xml._

class editXML {

  def addObject() {

    var device = XML.load("src/main/scala/PrivateHome/devices.xml")
    //var idXml = XML.load("src/main/scala/PrivateHome/id.xml")

    val name = "object.getName()"
    val id = "object.getId()"
    val room = "object.getRoom()"
    val typE = "object.getType()"
    //if(object.getType == 443){
    //    val systemCode = object.getSystemCode()
    //    val unitCode = object.getUnitCode()
    //    }
    val systemCode = "object.getSystemCode()"
    val unitCode = "object.getUnitCode()"
    val test = <object id ={id}><name>+ name +</name><room>+ room +</room><type>+ typE +</type><systemCode>+ systemCode +</systemCode><unitCode>+ unitCode +</unitCode></object>
    val result = addANode(device, test)


    XML.save("src/main/scala/PrivateHome/devices.xml", result)
    // "<object id='" + id +"'><name>"+ name + "</name><room>"+room+"</room><type>"+typE+"</type><systemCode>"+systemCode+"</systemCode><unitCode>"+unitCode+"</unitCode></object></device>"
  }
  def addANode (to: Node, newNode: Node) = to match {
    case Elem (prefix, label, attributes, scope, child@_*) => Elem (prefix, label, attributes, scope, child ++ newNode: _*)
    case _ => println(" could not find node ");
  to
  }
}

