package PrivateHome

//ToDo: make settings for an config file
case object settings {
  var websocket: http = new http(port = 2888,"/ws")
  var http:http = new http(2000,"./src/main/scala/PrivateHome/UI/GUI/Website") //ToDo: change to 80 in produktion
  var database = new database(userName = "user", password = "pass","./daten/Devices")
  var mqtt = mqttBroker("localhost",1500)
}

case class http(var port:Int,var path:String="") {if (port<0||port>0xffff) throw new IllegalArgumentException("Argument Port aut of bound must be between 0x0 and 0xffff=65536")}
case class database(var userName:String,var password: String,var path:String)
case class mqttBroker(var url:String, var port:Int) {if (port<0||port>0xffff) throw new IllegalArgumentException("Argument Port aut of bound must be between 0x0 and 0xffff=65536")}

