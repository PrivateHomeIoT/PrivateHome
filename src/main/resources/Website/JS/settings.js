var oldid;

ws.onmessage = function(event) {
  var msg = JSON.parse(event.data);
  switch (msg.Command) {
    case "getDevices": {

      break;
    }
    case "getDevice":{
      fillForm(msg.answer);
      break;
    }
    case "newDevice": {
      break;
    }
    case "deviceChange":{
      fillForm(msg.answer);
      window.location.search = "id=${msg.answer.id}"
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
  x = document.getElementById("switchform").masterId
  for (controller of args) {
    var option = document.createElement("option");
    option.value = controller.masterId;
    option.text = controller.masterId + ": " + controller.name;
    x.add(option);
  }
}

function fillForm(args) {
  let answer = args;
  oldid = answer.id
  form = document.getElementById("switchform")
  form.keepState.checked = answer.keepState;
  form.name.value = answer.name;
  form.controlType.checked = answer.controlType == "slider";
  form.switchType.disabled = form.controlType.checked;
  form.switchType.value = answer.switchType;

  disable433 = answer.switchType != "433Mhz";
  form.unitCode.disabled = disable433;
  form.systemCode.disabled = disable433;
  if (!disable433) {
    form.systemCode.value = answer.systemCode;
    form.unitCode.value = answer.unitCode;
  } else {
    form.systemCode.value = "";
    form.unitCode.value = "";
  }

  disableMqtt = answer.switchType != "mqtt";
  form.pin.disabled = disableMqtt;
  form.masterId.disabled = disableMqtt;
  if (disableMqtt) {
    form.masterId.value = "";
  } else {
    form.pin.value = answer.pin
    form.masterId.value = answer.masterId
  }
}


ws.onopen = function (event) {
  const queryString = window.location.search;
  const urlParams = new URLSearchParams(queryString);
  if(urlParams.has("id")) {
    ws.send(JSON.stringify({Command:"getController"}));
    ws.send(JSON.stringify({Command:"getDevice",Args:{id:urlParams.get("id")}}));
  }

}
var argus

function sendNewData() {
  form = document.getElementById("switchform")
  var arguments = new Object();
  arguments.oldId = oldid;
  arguments.newId = form.idtext.value;
  arguments.keepState = form.keepState.checked;
  arguments.name = form.name.value;
  if (form.controlType.checked) {
    arguments.controlType = "slider";
    arguments.switchType = "mqtt";
  } else {
    arguments.controlType = "button";
    arguments.switchType = form.switchType.value;
    if (arguments.switchType == "mqtt") {
      arguments.pin = parseInt(form.pin.value);
      arguments.masterId = form.masterId.value
    } else {}
      arguments.systemCode = form.systemCode.value;
      arguments.unitCode = form.unitCode.value;
    }

  console.log(JSON.stringify(arguments));
  var message = new Object();
  message.Args = arguments;
  message.Command = "updateDevice";
  ws.send(JSON.stringify(message));
}


function registerHandler() {
  form = document.getElementById("switchform")
  form.addEventListener( "submit", function ( event ) {
    event.preventDefault();
    sendNewData();
  });
  form.controlType.addEventListener("change", function (event) {
    disable = form.controlType.checked
    form.systemCode.disabled = disable;
    form.unitCode.disabled = disable;
    form.switchType.disabled = disable;
    if (disable) {
    form.switchType.value = "mqtt";
    form.unitCode.value = "";
    form.systemCode.value = "";
    }
  });
  form.switchType.addEventListener("change", function (event) {
    disable433 = form.switchType.value != "433Mhz";
    form.systemCode.disabled = disable433;
    form.unitCode.disabled = disable433;
    if (disable433) {
      form.unitCode.value = "";
      form.systemCode.value = "";
    }
    disableMqtt = answer.switchType != "mqtt";
    form.pin.disabled = disableMqtt;
    form.masterId.disabled = disableMqtt;
  });
}


window.addEventListener('load', function () {
  const queryString = window.location.search;
  const urlParams = new URLSearchParams(queryString);
  document.getElementById("idtext").value = urlParams.get("id");
  registerHandler();
});