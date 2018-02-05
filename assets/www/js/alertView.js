
var globalElements = {

	interval : undefined

};

var setEnvironment = {

	counter: 30,

	initialize: function() {
		this.bindEvents();
	},

	bindEvents: function() {
		document.addEventListener("deviceready", this.onDeviceReady, false);
	},

	onDeviceReady: function() {

		globalElements.interval = setInterval(function() {
			document.getElementById("temp").innerHTML = setEnvironment.counter.toString();

			if (setEnvironment.counter==0) {
				clearInterval(globalElements.interval);
				document.getElementById("tempText").innerHTML = "Enviando alerta...";

				cordova.exec(false, false, "LinkToService", "sendAlert", []);
			}

			setEnvironment.counter--;
		}, 1000);
	}
};


var functionality = {

	arg : '{"openMainPage": "true"}',

	no : function() {
		clearInterval(globalElements.interval);
		cordova.exec(false, false, "LinkToService", "returnToMainPage", []);
	},

	yes : function() {
		clearInterval(globalElements.interval);
		cordova.exec(false, false, "LinkToService", "sendAlert", [JSON.parse(functionality.arg)]);
	},
};

setEnvironment.initialize();