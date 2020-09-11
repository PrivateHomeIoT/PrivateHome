
        var ws = new WebSocket(location.hostname + ":2888");

        ws.onmessage = function(event) {
            var msg = JSON.parse(event.data);
            switch (msg.type) {
                case "id":
                    if (msg.status == 1) {
                        onGraph(msg.id);
                    } else if (msg.status == 0) {
                        offGraph(msg.id);
                    } else {
                        document.getElementById(ID + "sliderValue").innerHTML = msg.status.toInteger() * 100;
                        document.querySelector('#' + ID + "slider").value = msg.status.toInteger() * 100;
                    }
                case "devices":
                    var devices = JSON.parse(msg.devices);
                    var i;
                    while (i < Object.keys(devices).length) {
                        var act = devices[i];
                        var id = act.id;
                        var name = act.name;
                        var status = act.status * 100;
                        var type = act.type;

                        if (status == 0) {
                            try {
                                offGraph(id);
                                document.getElementById(id + "name").innerHTML = name;
                            } catch {
                                if (type == "button") generateButton(id, name, status);
                                else if (type == "slider") generateSlider (id, name, status);
                            }
                        } else if (status == 1) {
                            try {
                                onGraph(id);
                                document.getElementById(id + "name").innerHTML = name;
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
                case "error": console.log(msg.error.toString);

                case: console.log("Illegal Argument");
            }
        }

        function generateButton(String id, String name, float status){
            var text, html;
            text="<td class=\"switch\" id="+id+"\><button id="id + "on\" class=\"button\" style=\"display: none;\" onclick=\"turnOff('"+id+"')\"><img src=\"Pictures/on.png\" alt=\"ON\" class=\"pic\"></button><button id="+id+"off\" class=\"button\" style=\"display: inline-block;\" onclick=\"turnOn('"id"')\"><img src=\"Pictures/off.png\" alt=\"OFF\" class=\"pic\"></button><a href=\"Settings/"+id+".html\" class=\"aButton\"><p id=\"name\">Name</p><p id=\"status\">Status</p></a></td>";
            html = document.getElementById('actions');
            html.innerHTML(text);
        }
        
        function generateSlider(String id, String name, float status){
            var text, html;
            text="<td class=\"switch\" id=\""+id+"\"><input class=\"slider button\" id=\""+id+"slider\" onchange=\"slider('"id"')\" max=\"100\" min=\"0\" type=\"range\" value=\""+status*100+"\"><a href=\"Settings/"+id+".html\" class=\"aButton\"><p id=\""+id+"\">"+name+"</p><p id=\""+id+"status\">Status</p></a><p class=\"slidervalue\" id=\""+id+"sliderValue\">"+value*100+"%</p></td>";
            html = document.getElementById('actions');
            html.innerHTML(text);
        }
        
        /*
            This function turns on the graphic interface of a switch or slider.
            Usage: onGraph(String id);
        */

        function onGraph() {
            var ID = turnOn.arguments[0];
            try {
                document.getElementById(ID + "off").style.display = 'none';
                document.getElementById(ID + "on").style.display = 'inline-block';
            } catch {
                document.getElementById(ID + "sliderValue").innerHTML = "100 %";
                document.querySelector('#' + ID + "slider").value = 100;
            }
            document.getElementById(id + "status").innerHTML = "ON";
        }

        /*
            This function turns off the graphic interface of a switch or slider.
            Usage: offGraph(String id);
        */
        function offGraph() {
            var ID = turnOn.arguments[0];
            try {
                document.getElementById(ID + "on").style.display = 'none';
                document.getElementById(ID + "off").style.display = 'inline-block';
            } catch {
                document.getElementById(ID + "sliderValue").innerHTML = "0 %";
                document.querySelector('#' + ID + "slider").value = 0;
            }
            document.getElementById(id + "status").innerHTML = "OFF";
        }

        /*
            This function send an on signal to the websocket.
            Usage: turnOn(String id);
        */
        function turnOn() {
            var ID = turnOn.arguments[0];
            ws.send(JSON.stringify({
                Command: "ON",
                Args: {
                    Percent: 1,
                    id: ID
                }
            }));
        }

        /*
            This function sends the value of a slider to the websocket.
            Usage: slider(String id);
        */
        function slider() {
            var ID = slider.arguments[0];
//            document.getElementById(ID + "sliderValue").innerHTML = document.querySelector('#' + ID + "slider").value + " %";
            ws.send(JSON.stringify({
                Command: "OFF",
                Args: {
                    Percent: document.querySelector('#' + ID + "slider").value / 100,
                    id: ID
                }
            }));
        }

        /*
            This function send an off signal to the websocket.
            Usage: turnOff(String id);
        */
        function turnOff() {
            var ID = turnOff.arguments[0];
            ws.send(JSON.stringify({
                Command: "OFF",
                Args: {
                    Percent: 0,
                    id: ID
                }
            }));
        }
