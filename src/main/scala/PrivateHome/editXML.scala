package PrivateHome

import scala.xml._

class editXML {

  def addObject() {

    var device = XML.load("src/main/scala/PrivateHome/devices.xml")
    var idXml = XML.load("src/main/scala/PrivateHome/id.xml")
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
    val id_result = addANode(idXml,id_prep)
    XML.save("src/main/scala/PrivateHome/id.xml", id_result)

    val prep = <object id ={id}>&#xD;&#9;<name>{name}</name>&#xD;&#9;<room>{room}</room>&#xD;&#9;<type>{typE}</type>&#xD;&#9;<systemCode>{systemCode}</systemCode>&#xD;&#9;<unitCode>{unitCode}+</unitCode>&#xD;&#9;</object>
    val result = addANode(device, prep)
    XML.save("src/main/scala/PrivateHome/devices.xml", result)
  }

  def addANode (to: Node, newNode: Node) = to match {
    case Elem (prefix, label, attributes, scope, child@_*) => Elem (prefix, label, attributes, scope, child ++ newNode: _*)
    case _ => println(" could not find node ");
  to
  }
}

