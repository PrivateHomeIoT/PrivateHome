var randomID;

ws.onmessage = function(event) {
  var msg = JSON.parse(event.data);
  switch (msg.Command) {
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
    default: { console.log(msg.error.toString() + ": " + msg.exception);}
  }
}

function setID(args){
  randomID = args.id;
  document.getElementById("idtext").value = randomID
}

ws.onopen = function (event) {
  const queryString = window.location.search;
  const urlParams = new URLSearchParams(queryString);
  ws.send(JSON.stringify({Command:"getRandomId",Args:{}}));
}

function sendNewData() {
  form = document.getElementById("form")
  var arguments = new Object();
  arguments.randomID = randomID;
  arguments.newId = form.idtext.value;
  arguments.keepState = form.keepState.checked;
  arguments.name = form.name.value;
  if (form.controlType.checked) {
    arguments.controlType = "slider";
    arguments.switchType = "mqtt";
  } else {
    arguments.controlType = "button";
    arguments.switchType = form.switchType.value;
    arguments.systemCode = form.systemCode.value;
    arguments.unitCode = form.unitCode.value;
  }
  console.log(JSON.stringify(arguments));
  ws.send(JSON.stringify({Command:"createDevice",Args:arguments}));
}


function registerHandler() {
  form = document.getElementById("form")
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
    disable = form.switchType.value != "433Mhz";
    form.systemCode.disabled = disable;
    form.unitCode.disabled = disable;
    if (disable) {
      form.unitCode.value = "";
      form.systemCode.value = "";
    }
  });
}

window.addEventListener('load', function () {
  const queryString = window.location.search;
  const urlParams = new URLSearchParams(queryString);
  document.getElementById("idtext").value = urlParams.get("id");
  registerHandler();
});