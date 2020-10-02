var ws = new WebSocket("ws://" + location.hostname + ":2888/ws");

ws.onmessage = function(event) {
  var msg = JSON.parse(event.data);
  switch (msg.Command) {
    case "statusChange":
    
    if (msg.answer.type == "Button") {
      if (msg.status == 1) {
        onGraph(msg.id);
      } else if (msg.status == 0) {
        offGraph(msg.id);
      }
    } else {
      setSlider(id,msg.answer.status*100)
    }
    break;
    case "getDevices":
      var devices = msg.answer.devices

      for (act of devices) {
        var id = act.id;
        var name = act.name;
        var status = act.status * 100;
        var type = act.controlType;

        if (document.getElementById(id)) {
          if (type=="button") {
            if (status == 0) {
              offGraph(id);
            } else {
              onGraph(id);
            }
          } else {
            setSlider(id,status);
          }
        } else {
          if (type == "button") {generateButton(id,name,status)}
          else {
            generateSlider(id,name,status)
          }
        }
      }
      break;
    default: { console.log(msg.error.toString() + ": " + msg.exception);}
  }
}

/**
This function adds a Button
**/
function generateButton(id, name, status){
  var html;
  if (status) {status = "ON"}
  else {status = "OFF"}
  var Button =
  `<td class="switch" id="${id}">
  <button id="${id}on" class="button" style="display: none" onclick="turnOff('${id}'')">
    <img src="Pictures/on.png" alt="ON" class="pic">
  </button>
  <button id="${id}off" class="button" style="display: inline-block" onclick="turnOn('${id}')">
    <img src="Pictures/off.png" alt="OFF" class="pic">
  </button>
  <a href="Settings/${id}.html" class="aButton">
    <p id="${id}Name">${name}</p>
    <p id="${id}Status">${status}</p>
  </a>
  </td>`
  html = document.getElementById('actions');
  html.innerHTML += Button;
}

/*
This function adds a Slider
*/
function generateSlider(id, name, status){
  var text, html;
  text =
  `<td class="switch" id="${id}">
  <input id="${id}slider" class="slider button" onchange="slider('${id}')" max="100" min="0" type="range" value="${status}">
  <a href="Settings/${id}.html" class="aButton">
    <p id="${id}Name">${name}</p>
    <p id="${id}Status">${status} %</p>
  </a>
  </td>`
  html = document.getElementById('actions');
  html.innerHTML += text;
}

/*
This function turns on the graphic interface of a switch.
Usage: onGraph(String id);
*/

function onGraph(ID) {
  document.getElementById(ID + "off").style.display = 'none';
  document.getElementById(ID + "on").style.display = 'inline-block';
  document.getElementById(ID + "status").innerHTML = "ON";
}

/*
This function turns off the graphic interface of a switch.
Usage: offGraph(String id);
*/
function offGraph(ID) {
  document.getElementById(ID + "on").style.display = 'none';
  document.getElementById(ID + "off").style.display = 'inline-block';
  document.getElementById(ID + "Status").innerHTML = "OFF";
}

function setSlider(ID,Status) {
  document.getElementById(id+"slider").value  = Status;
  document.getElementById(id+"Status").innerHTML = status + " %";
}

/*
This function send an on signal to the websocket.
Usage: turnOn(String id);
*/
function turnOn(ID) {
  console.log(ID);
  ws.send(JSON.stringify({
    Command: "on",
    Args: {
      percent: "1",
      id: ID
    }
  }));
}

/*
This function sends the value of a slider to the websocket.
Usage: slider(String id);
*/
function slider(ID) {
  //            document.getElementById(ID + "sliderValue").innerHTML = document.querySelector('#' + ID + "slider").value + " %";
  ws.send(JSON.stringify({
    Command: "on",
    Args: {
      percent: (document.getElementById(ID + "slider").value / 100).toString(),
      id: ID
    }
  }));
}

/*
This function send an off signal to the websocket.
Usage: turnOff(String id);
*/
function turnOff(ID) {
  ws.send(JSON.stringify({
    Command: "OFF",
    Args: {
      Percent: 0,
      id: ID
    }
  }));
}

ws.onopen = function (event) {
  fill();
}
