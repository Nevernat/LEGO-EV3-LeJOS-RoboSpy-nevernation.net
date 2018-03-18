var connection;

function EV3_connect() {
    var addr = document.getElementById("remote_adr").value;

    connection = new WebSocket("ws:/" + addr + ":80");
    document.getElementById("video").src = "http://" + addr + ":8080";
    connection.onmessage = function(e) {
        if (e.data.substring(0, 3) == "#d#") {
            var dist = e.data.split("d#")[1];
            if (dist == "Infinity") dist = 100;
            else dist = parseFloat(dist);
            setRange(dist);
        }else{
            document.getElementById("log").innerHTML = e.data;
        }
    };
    connection.onerror = function(e) {
        console.log("error", e.data);
    };
    connection.onopen = function() {
        setConnected(true);
        console.log("connected");
    };
};

function EV3_send(msg) {
    connection.send(msg);
}

function btn_stop_pressed(state) {
    if (state) document.getElementById("btn_stop").src = "img/button_stop_on.png";
    else document.getElementById("btn_stop").src = "img/button_stop.png";
}

function stop() {
    EV3_send("stop");
    document.getElementById("motor_up").src = "img/btn_up_off.png";
    document.getElementById("motor_down").src = "img/btn_down_off.png";
    document.getElementById("motor_lef").src = "img/btn_splef_off.png";
    document.getElementById("motor_rig").src = "img/btn_sprig_off.png";
    document.getElementById("cam_up").src = "img/btn_up_off.png";
    document.getElementById("cam_down").src = "img/btn_down_off.png";
    document.getElementById("cam_lef").src = "img/btn_lef_off.png";
    document.getElementById("cam_rig").src = "img/btn_rig_off.png";
}

function motor(direction) {
    if (direction == "up") {
        EV3_send("go_forw");
        document.getElementById("motor_up").src = "img/btn_up_on.png";
    }
    if (direction == "down") {
        EV3_send("go_back");
        document.getElementById("motor_down").src = "img/btn_down_on.png";
    }
    if (direction == "lef") {
        EV3_send("go_left");
        document.getElementById("motor_lef").src = "img/btn_splef_on.png";
    }
    if (direction == "rig") {
        EV3_send("go_right");
        document.getElementById("motor_rig").src = "img/btn_sprig_on.png";
    }
}

function cam(direction) {
    if (direction == "up") {
        EV3_send("cam_up");
        document.getElementById("cam_up").src = "img/btn_up_on.png";
    }
    if (direction == "down") {
        EV3_send("cam_down");
        document.getElementById("cam_down").src = "img/btn_down_on.png";
    }
    if (direction == "lef") {
        EV3_send("cam_left");
        document.getElementById("cam_lef").src = "img/btn_lef_on.png";
    }
    if (direction == "rig") {
        EV3_send("cam_right");
        document.getElementById("cam_rig").src = "img/btn_rig_on.png";
    }
}

function speed(val) {
    for (var i = 1; i <= 5; i++) {
        if (val >= i) document.getElementById("speed_" + i).src = "img/btn_small_on_" + i + ".png";
        else document.getElementById("speed_" + i).src = "img/btn_small_off_" + i + ".png";
    };
    EV3_send("speed_" + val);
}

function setRange(val) {
    // value 0:50 (100 infinity)
    // px: 10: 172

    if (val == 100)
        document.getElementById("bar_radar").style.visibility = "hidden";
    else {
        var top = 178 - (Math.floor((val * 162) / 50) + 10);
        document.getElementById("bar_radar").style.top = top + "px";
        document.getElementById("bar_radar").style.visibility = "visible";
    }

}

function setConnected(state) {
    if (state) document.getElementById("led_connected").style.visibility = "visible";
    else document.getElementById("led_connected").style.visibility = "hidden";
}

function deploy() {
    setDeployState(0);
    EV3_send("deploy");
    setDeployState(1);
}

function undeploy() {
    setDeployState(0);
    EV3_send("undeploy");
    setDeployState(2);
}

function setDeployState(state3) {
    // 0: deploying
    // 1: deployed
    // 2: undeployed
    if (state3 == 0) {
        document.getElementById("led_deployed").style.visibility = "hidden";
        document.getElementById("led_undeployed").style.visibility = "hidden";
    }
    if (state3 == 1) {
        document.getElementById("led_deployed").style.visibility = "visible";
        document.getElementById("led_undeployed").style.visibility = "hidden";
    }
    if (state3 == 2) {
        document.getElementById("led_deployed").style.visibility = "hidden";
        document.getElementById("led_undeployed").style.visibility = "visible";
    }
}
