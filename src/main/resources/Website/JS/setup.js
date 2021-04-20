var randomID;

ws.onmessage = function(event) {
  var msg = JSON.parse(event.data);
  switch (msg.Command) {
    case "addDevice": {
      if (msg.answer == "Success") {
        window.location.href = "/Devices.html"
      }
      break;
    }
    case "getDevices": {
      break;
    }
    case "getDevice":{
      break;
    }
    case "newDevice": {
      break;
    }
    case "deviceChange":{
      break;
    }
    case "getRandomId":{
      setID(msg.answer);
      break;
    }
    case "getController": {
      setController(msg.answer)
      break;
    }
    default: { console.log(msg.error.toString() + ": " + msg.exception);}
  }
}

function setController(args) {
  x = document.getElementById("masterId");
  while (x.length > 0){
    x.remove(0)
  }
  for (controller of args) {
    var option = document.createElement("option");
    option.value = controller.masterId;
    option.text = controller.masterId + ": " + controller.name;
    x.add(option);
  }
}

function setID(args){
  randomID = args.id;
  document.getElementById("idtext").value = randomID
  controlTypeCheck();

}

ws.onopen = function (event) {
  const queryString = window.location.search;
  const urlParams = new URLSearchParams(queryString);
  ws.send(JSON.stringify({Command:"getRandomId",Args:{}}));
  ws.send(JSON.stringify({Command:"getController",Args:{}}));
}

function sendNewData() {
  form = document.getElementById("form")
  var arguments = new Object();
  arguments.randomID = randomID;
  arguments.id = form.idtext.value;
  arguments.keepState = form.keepState.checked;
  arguments.name = form.name.value;
  if (form.controlType.checked) {
    arguments.controlType = "slider";
    arguments.switchType = "mqtt";
  } else {
    arguments.controlType = "button";
    arguments.switchType = form.switchType.value;
    }
    if (arguments.switchType == "mqtt") {
      arguments.pin = parseInt(form.pin.value);
      arguments.masterId = form.masterId.value
    } else {
      arguments.systemCode = form.systemCode.value;
      arguments.unitCode = form.unitCode.value;
    }
  console.log(JSON.stringify(arguments));
  ws.send(JSON.stringify({Command:"addDevice",Args:arguments}));
}


function registerHandler() {
  form = document.getElementById("form")
  form.addEventListener( "submit", function ( event ) {
    event.preventDefault();
    sendNewData();
  });
  form.controlType.addEventListener("change", function (event) {
    form = document.getElementById("form")
    disable = form.controlType.checked
    form.switchType.disabled = disable;
    if(disable) {
      form.switchType.value = "mqtt"
    }
    controlTypeCheck();
  });
  form.switchType.addEventListener("change", controlTypeCheck);
}

function controlTypeCheck() {
  form = document.getElementById("form")
      disable433 = form.switchType.value != "433Mhz";
      form.systemCode.disabled = disable433;
      form.unitCode.disabled = disable433;
      if (disable433) {
        form.unitCode.value = "";
        form.systemCode.value = "";
      }
      disableMqtt = form.switchType.value != "mqtt";
      form.pin.disabled = disableMqtt;
      form.masterId.disabled = disableMqtt;
}

window.addEventListener('load', function () {
  const queryString = window.location.search;
  const urlParams = new URLSearchParams(queryString);
  document.getElementById("idtext").value = urlParams.get("id");
  registerHandler();
});