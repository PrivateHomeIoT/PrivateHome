var ws = new WebSocket("ws://" + location.hostname + ":2888/ws");

ws.onmessage = function(event) {
  var msg = JSON.parse(event.data);
    if (msg.status == 1) {
      onGraph(msg.id);
    } else if (msg.status == 0) {
      offGraph(msg.id);
  switch (msg.Command) {
    case "statusChange":
    } else {
      document.getElementById(ID + "sliderValue").innerHTML = msg.status.toInteger() * 100;
      document.querySelector('#' + ID + "slider").value = msg.status.toInteger() * 100;
    }
    break;
    case "getDevices":
      var devices = msg.answer.devices

      for (act of devices) {
        var id = act.id;
        var name = act.name;
        var status = act.status * 100;
        var type = act.controlType;

        if (status == 0) {
          try {
            offGraph(id);
            document.getElementById(id + "Name").innerHTML = name;
          } catch {
            if (type == "button") generateButton(id, name, status);
            else if (type == "slider") generateSlider (id, name, status);
          }
        } else if (status == 1) {
          try {
            onGraph(id);
            document.getElementById(id + "Name").innerHTML = name;
          } catch {
            if (type == "button") generateButton(id, name, status);
            else if (type == "slider") generateSlider (id, name, status);
          }
        } else {
          try{
            document.getElementById(ID + "sliderValue").innerHTML = status + " %";
            document.querySelector('#' + ID + "slider").value = status;
            document.getElementById(id + "status").innerHTML = "ON";
            document.getElementById(id + "name").innerHTML = name;
          } catch {
            if (type == "button") generateButton(id, name, status);
            else if (type == "slider") generateSlider (id, name, status);
          }
        }
      }
    }
    else if (msg.error) { console.log(msg.error.toString() + ": " + msg.exception);}


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
This function turns on the graphic interface of a switch or slider.
Usage: onGraph(String id);
*/

function onGraph(ID) {
  try {
    document.getElementById(ID + "off").style.display = 'none';
    document.getElementById(ID + "on").style.display = 'inline-block';
    document.getElementById(ID + "status").innerHTML = "ON";
  } catch {
    document.getElementById(ID + "status").innerHTML = "100 %";
    document.getElementById(ID + "slider").value = 100;
  }
}

/*
This function turns off the graphic interface of a switch or slider.
Usage: offGraph(String id);
*/
function offGraph(ID) {
  console.log(ID)
  try {
    document.getElementById(ID + "on").style.display = 'none';
    document.getElementById(ID + "off").style.display = 'inline-block';
    document.getElementById(ID + "Status").innerHTML = "OFF";
  } catch (error) {
    document.getElementById(ID + "slider").value = 0;
    document.getElementById(ID + "Status").innerHTML = "0 %";
  }
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
