package PrivateHome

//ToDo: make settings for an config file
case object settings {
  var websocket: http = new http(port = 2888)
  var http:http = new http(2000) //ToDo: change to 80 in produktion
  var database = new database(userName = "user", password = "pass")
}
case class http(port:Int)
case class database(userName:String,password: String)

