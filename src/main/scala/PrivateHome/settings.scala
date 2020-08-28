package PrivateHome

//ToDo: make settings for an config file
case object settings {
  var websocket: http = new http(port = 2888)
  var http:http = new http(2000) //ToDo: change to 80 in produktion
  var database = new database(userName = "user", password = "pass")
}

case class http(port:Int) {if (port<0||port>0xffff) throw new IllegalArgumentException("Argument Port aut of bound must be between 0x0 and 0xffff=65536")}
case class database(userName:String,password: String)


