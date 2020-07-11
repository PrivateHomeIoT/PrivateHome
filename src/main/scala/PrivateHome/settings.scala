package PrivateHome

//ToDo: make settings for an config file
case object settings {
  var http: http = new http(port = 2888)
  var database = new database(userName = "user", password = "pass")
}

private case class http(port:Int)
private case class database(userName:String,password: String)

