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
    default: { console.log(msg.error.toString() + ": " + msg.exception);}
  }
}

function fillForm(args) {
  let answer = args;
  oldid = answer.id
  form = document.getElementById("form")
  form.keepState.checked = answer.keepState;
  form.name.value = answer.name;
  form.controlType.checked = answer.controlType == "slider";
  form.switchType.value = answer.switchType;
  disable433 = answer.switchType != "433Mhz"
  form.unitCode.disabled = disable433;
  form.systemCode.disabled = disable433;
  form.switchType.disabled = form.controlType.checked;
  if (!disable433) {
    form.systemCode.value = answer.systemCode;
    form.unitCode.value = answer.unitCode;
  } else {
    form.systemCode.value = "";
    form.unitCode.value = "";
  }
}


ws.onopen = function (event) {
  const queryString = window.location.search;
  const urlParams = new URLSearchParams(queryString);
  if(urlParams.has("id")) {
    ws.send(JSON.stringify({Command:"getDevice",Args:{id:urlParams.get("id")}}));
  }

}

function sendNewData() {
  form = document.getElementById("form")
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
    arguments.systemCode = form.systemCode.value;
    arguments.unitCode = form.unitCode.value;
  }
  console.log(JSON.stringify(arguments));
  ws.send(JSON.stringify({Command:"updateDevice",Args:arguments}));
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