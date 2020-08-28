package PrivateHome

//ToDo: make settings for an config file
case object settings {
  var http : http = new http(port = 3143)
  var database : database = new database(userName = "user", password = "pass")
}
case class http(port:Int)
case class database(userName:String,password: String)

