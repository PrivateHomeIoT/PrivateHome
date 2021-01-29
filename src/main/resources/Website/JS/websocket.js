
class WebSocketControll {

  sessionId = "";
  authenticated = false;
  connection = null;
  url = "";


  constructor(url) {
    this.url = url
  }

  authenticate() {
    console.log(this.authenticated)
    this.sessionId = window.sessionStorage.getItem('sessionId');
    console.log(this.sessionId)
    if (this.sessionId) {
      console.log("test for senden")
      this.send(JSON.stringify({"auth" : "ID", "sessionID" : this.sessionId}));
      console.log("test nach senden")
    } else {
      var username = prompt("Enter username");
      var pass = prompt("Enter password");
      this.send(JSON.stringify({"auth" : "pass", "username" : username, "pass" : pass}));
    }
    this.connection.onmessage = function (event) {ws.sessionauthenticationHandler(event);};
    console.log("set message handler")
  }

  sessionauthenticationHandler(event) {
    var msg = JSON.parse(event.data);
    console.log(msg)
    if (msg.auth != undefined) {
      console.log("Test")
      if (msg.authenticated) {
        window.sessionStorage.setItem("sessionId", msg.sessionID);
        ws.connection.onmessage = function (event) {ws.onmessage(event)};
        ws.onopen();
      } else {
        if (msg.auth ==="ID") {
          window.sessionStorage.removeItem("sessionId");
          this.authenticate();
        } else if (msg.auth === "pass") {
          alert("Username or password wrong!");
          this.authenticate();
        }
      }
    }
  }

  onmessage = null

  send(message) {
    if (this.connection.readyState == 1) {
      this.connection.send(message)
    } else {
      console.log(this.connection)
    }
  }

  onerror(event) {
  }

  onclose(event) {
    console.log(event.code)
    console.log(event.reason)
    if (event.code == 1015) {
      alert("There was an Error in the TLS handshake for WebSocket probaly because you use a self-singed certificate. When you close this Allert you get redirected to the WebSocket endpoint were you should add an exception. If you have never added an exception for this side contact the site maintainer.")
      window.location = `https://${location.hostname}:2888/test`
    }
    this.setup(this.connection.url);
  }

  setup(url) {
    this.connection = new WebSocket(url);
    this.connection.onopen = function (event) {ws.authenticate();};  // I don't know why I can not simply say onopen = this.authenticate but then this gets executed instantly and not wen the connection is established
    this.connection.onerror = function (event) {ws.onerror(event);}
    this.connection.onclose = function (event) {ws.onclose(event);}
  }
}

window.addEventListener('load', function () {

  ws.setup("wss://" + location.hostname + ":2888/ws");
})
var ws = new WebSocketControll();
