

ws.onmessage = function(event) {
  var msg = JSON.parse(event.data);
  switch (msg.Command) {
    case "statusChange":
    if (msg.answer.type == "Button") {
      if (msg.answer.status == 1) {
        onGraph(msg.answer.id);
      } else if (msg.answer.status == 0) {
        offGraph(msg.answer.id);
      }
    } else {
      setSlider(msg.answer.id,msg.answer.status*100)
    }
    break;
    case "getDevices": {
      var devices = msg.answer.devices
      for (act of devices) {
        var id = act.id;
        var name = act.name;
        var status = act.status * 100;
        var dimmable = act.dimmable;
        switchGenerator(id,name,status,dimmable);

      }
      break;
    }
    case "getDevice":{
      let answer = msg.answer;
      switchGenerator(answer.id,answer.name,answer.status*100,answer.controlType);
      break;
    }
    case "newDevice": {
      let answer = msg.answer;
      switchGenerator(answer.id,answer.name,answer.status*100,answer.controlType);
    }
    default: { console.log(msg.error.toString() + ": " + msg.exception);}
  }
}


/**
This function test if a switch with this id exist and than changes the status of it or if not it generates it.
*/

//TODO: should rename this function
function switchGenerator(id,name,status,dimmable) {
    if (document.getElementById(id)) {
        if (!dimmable) {
            if (status == 0) {
                offGraph(id);
            } else {
                onGraph(id);
             }
        } else {
            setSlider(id,status);
        }
    } else {
        if (!dimmable) {generateButton(id,name,status)}
        else {
            generateSlider(id,name,status)
        }
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
  <button id="${id}on" class="button" style="display: ${(status == "ON") ? "inline-block" : "none"}" onclick="turnOff('${id}')">
    <img src="Pictures/on.png" alt="ON" class="pic">
  </button>
  <button id="${id}off" class="button" style="display: ${(status == "OFF") ? "inline-block" : "none"}" onclick="turnOn('${id}')">
    <img src="Pictures/off.png" alt="OFF" class="pic">
  </button>
  <a href="Settings.html?id=${id}" class="aButton">
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
  <a href="Settings.html?id=${id}" class="aButton">
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
  document.getElementById(ID + "Status").innerHTML = "ON";
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
  document.getElementById(ID+"slider").value  = Status;
  document.getElementById(ID+"Status").innerHTML = Status + " %";
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
    Command: "off",
    Args: {
      percent: 0,
      id: ID
    }
  }));
}

ws.onopen = function (event) {
  ws.send(JSON.stringify({Command:"getDevices"}));
}
