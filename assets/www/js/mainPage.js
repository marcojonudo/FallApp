
var globalElements = {

	swypeRight : $("#swypeRight")

};

var setEnvironment = {

	initialize: function() {
		this.bindEvents();
	},

	bindEvents: function() {
		document.addEventListener("deviceready", this.onDeviceReady, false);
	},

	onDeviceReady: function() {
		FastClick.attach(document.body);
		setEnvironment.receivedEvent();
	},

	receivedEvent: function() {
		$("#menuContent a").eq(0).addClass("activeOption");
		setEnvironment.setListeners();
	},

	setListeners : function() {
		globalElements.swypeRight
			.on("touchstart", menuFunctions.touchStart)
			.on("touchmove", menuFunctions.touchMove)
			.on("touchend", menuFunctions.touchEnd);

		$(document).on("ready", menuFunctions.getVariables());

		$("body").addEventListener("load", menuFunctions.setIScroll());
	}
};

var menuFunctions = {

	elements : {
		mainWindow : $("#mainWindow"),
		darken : $("#darken"),
		head : $("head"),
		mainContent : $("#mainContent"),
		backMain : $("#backMain"),
		menuElements : $("#menuContent").find("a")
	},

	var : {
		slideState : "waitingTouch",
		visibleScreen : "mainWindow",
		startX : 0,
		distance : 0,
		startPixelOffset : 0,
		pixelOffset : 0,
		menuWidth : $("body").width() * 0.8,
		opacityPixel : 0.7 / ($("body").width() * 0.8),
		actualOpacity : 0,
		style : undefined
	},

	touchStart : function(event) {
		if (event.originalEvent.touches) {
			event = event.originalEvent.touches[0];
		}

		menuFunctions.var.startX = event.clientX;

		if ((menuFunctions.var.slideState == "waitingTouch")&&(menuFunctions.var.visibleScreen == "mainWindow")) {
			menuFunctions.var.slideState = "startTouch";
		}
		else {
			menuFunctions.elements.mainWindow
				.removeClass()
				.addClass("translate0 transition");

			menuFunctions.removeDarken(true);

			globalElements.swypeRight.css("width","3%");

			menuFunctions.var.visibleScreen = "mainWindow";
		}
	},

	touchMove : function(event) {
		event.preventDefault();

		if (event.originalEvent.touches) {
			event = event.originalEvent.touches[0];
		}

		menuFunctions.var.distance = event.clientX - menuFunctions.var.startX;

		if ((menuFunctions.var.slideState == "startTouch")&&(menuFunctions.var.distance != 0)) {
			menuFunctions.var.slideState = "movingSlide";
			menuFunctions.elements.darken
				.removeClass()
				.addClass("showDarken");
		}

		if (menuFunctions.var.slideState == "movingSlide") {
			menuFunctions.var.pixelOffset = menuFunctions.var.startPixelOffset + menuFunctions.var.distance;

			if ((menuFunctions.var.pixelOffset < menuFunctions.var.menuWidth)&&(menuFunctions.var.pixelOffset > 0)) {
				menuFunctions.elements.mainWindow
					.css("transform", "translate3d(" + menuFunctions.var.pixelOffset + "px, 0, 0)")
					.removeClass();

				menuFunctions.elements.darken
					.css("opacity", menuFunctions.var.opacityPixel * menuFunctions.var.pixelOffset);
			}
		}
	},

	touchEnd : function() {
		if (menuFunctions.var.slideState == "movingSlide") {

			menuFunctions.elements.mainWindow.css("transform", "");

			if (menuFunctions.var.pixelOffset > menuFunctions.var.menuWidth*0.3) {
				menuFunctions.elements.mainWindow
					.removeClass()
					.addClass("translate80 transition");

				menuFunctions.darken(false);

				globalElements.swypeRight.css("width", "100%");

				menuFunctions.var.visibleScreen = "menu";
			}
			else {
				menuFunctions.elements.mainWindow
					.removeClass()
					.addClass("translate0 transition");

				menuFunctions.removeDarken(false);
			}

			menuFunctions.var.slideState = "waitingTouch";
		}
		else {
			menuFunctions.var.slideState = "waitingTouch";
		}
	},

	setIScroll : function() {
		new IScroll("aboutContent");
	},

	getVariables : function() {
		cordova.exec(this.getVariablesSuccess, false, "LinkToService", "getVariables", []);
	},

	getVariablesSuccess : function(JSONVariables) {

		$("#name").html(JSONVariables["userName"]);

		var JSONNumbersArray = JSONVariables["telNumbers"];
		var numbersContainer = $("#numbers");

		var numbers = $(".numbers");
		for (var j=0; j<numbers.length; j++) {
			numbers.eq(j).remove();
		}
		for (var i=0; i<JSONNumbersArray.length; i++) {
			numbersContainer.append("<div class=numbers>" + JSONNumbersArray[i]["number" + (i+1).toString()] + "</div>");
		}

	},

	menu : function() {
		if (this.var.visibleScreen == "mainWindow") {
			this.elements.mainWindow
				.removeClass()
				.addClass("translate80 transition");

			this.darken(true);

			globalElements.swypeRight.css("width", "100%");

			this.var.visibleScreen = "menu";
		}
		else if (this.var.visibleScreen == "menu") {
			this.elements.mainWindow
				.removeClass()
				.addClass("translate0 transition");

			this.removeDarken(true);

			globalElements.swypeRight.css("width","3%");

			this.var.visibleScreen = "mainWindow";
		}
	},

	darken : function(fromMenu) {
		if (fromMenu) {
			this.elements.darken
				.removeClass()
				.addClass("showDarken setOpacity");
		}
		else {
			this.var.actualOpacity = this.elements.darken.css("opacity");

			var style = document.createElement("style");
			style.type = "text/css";
			style.innerHTML = "@-webkit-keyframes fadeinFromValue { from { opacity: " + this.var.actualOpacity + "; } to { opacity: 0.7;}}";
			document.getElementsByTagName("head")[0]
				.appendChild(style);

			this.elements.darken
				.removeClass()
				.addClass("showDarken setOpacityFromValue");

			this.elements.darken
				.one("webkitAnimationEnd", function() {
					document.getElementsByTagName("head")[0]
						.removeChild(style);
			});
		}
	},

	removeDarken : function(openedMenu) {

		if (openedMenu) {
			this.elements.darken
				.removeClass("setOpacity setOpacityFromValue")
				.addClass("removeOpacity");

			this.elements.darken
				.one("webkitAnimationEnd", function() {
					menuFunctions.elements.darken
						.removeClass("showDarken removeOpacity")
						.addClass("notShowDarken");
			});
		}
		else {
			this.var.actualOpacity = this.elements.darken.css("opacity");
			var style = document.createElement("style");
			style.type = "text/css";
			style.innerHTML = "@-webkit-keyframes fadeoutFromValue { from { opacity: " + this.var.actualOpacity + "; } to { opacity: 0;}}";
			document.getElementsByTagName("head")[0]
				.appendChild(style);

			this.elements.darken
				.removeClass("setOpacity setOpacityFromValue")
				.addClass("removeOpacityFromValue");

			this.elements.darken
				.one("webkitAnimationEnd", function() {
					document.getElementsByTagName("head")[0]
						.removeChild(style);

					menuFunctions.elements.darken
						.removeClass("showDarken removeOpacityFromValue")
						.addClass("notShowDarken");
			});
		}
	},

	menuContent : function(option) {

		this.elements.mainContent.addClass("notShow");

		var contentElements = $(".selection");

		for (var i=1; i<contentElements.length; i++) {
			contentElements.eq(i)
				.removeClass("show")
				.addClass("notShow");
		}

		contentElements.eq(option)
			.removeClass("notShow")
			.addClass("show");

		//La opcion de volver a principal no se muestra en la pantalla principal
		if (option == "0") {
			this.elements.backMain
				.removeClass("show")
				.addClass("notShow");
		}
		else {
			this.elements.backMain
				.removeClass("notShow")
				.addClass("show");
		}

		//Se limpia la opcion activa del menu
		for (i=0; i<this.elements.menuElements.length; i++) {
			this.elements.menuElements.eq(i)
				.removeClass("activeOption");
		}

		this.elements.menuElements.eq(option)
			.addClass("activeOption");

		this.elements.mainWindow
			.removeClass()
			.addClass("translate0 transition");

		this.removeDarken(true);

		globalElements.swypeRight.css("width","3%");

		this.var.visibleScreen = "mainWindow";
	}
};

var functionality = {

	var : {
		lastWrongTelNumber : 0,
		wrongTelNumber : 0,
		errorCode : -1,
		stop : false
	},

	changeSensibility : function() {
		functionality.showPrompt("promptBox");
	},

	showPrompt : function(element) {
		$("#" + element)
			.removeClass("notShow")
			.addClass("show");

		functionality.darkBackground();
	},

	updateRangeText : function(value) {
		$("#sensibilityValue").html(value);
	},

	sensibilityOK : function() {

		var argsSensibility = '{"newSensibility": "' + $("#sensibilityRange").val() + '"}';

		var sensibilityJSON = JSON.parse(argsSensibility);

		cordova.exec(false, false, "LinkToService", "updateSensibility", [sensibilityJSON]);

		functionality.cancel("promptBox");
	},

	changeNumbers : function() {
		var numbers = $(".numbers");
		var telNumbers = $(".number");

		for (var i=0; i<numbers.length; i++) {
			telNumbers.eq(i).val(numbers.eq(i).html());
			telNumbers.eq(i).removeClass("notShow").addClass("show");
		}

		functionality.showPrompt("telBox");
	},

	telOK : function() {
		if (functionality.validateNumbers()) {
			var telNumbers = $(".number");
			var loopLength = $(".numbers").length;

			var argsNumbers = '{"newTelNumbers": [';

			for (var i=0; i<loopLength; i++) {
				var number = "number" + (i+1);
				argsNumbers += '{"' + number + '": "' + telNumbers.eq(i).val() + '"},';
			}
			argsNumbers = argsNumbers.slice(0, argsNumbers.length - 1);

			argsNumbers += ']}';

			var numbersJSON = JSON.parse(argsNumbers);

			cordova.exec(menuFunctions.getVariablesSuccess, false, "LinkToService", "updateTelNumbers", [numbersJSON]);

			functionality.telCancel();
		}
		else {
			functionality.telNumberError(functionality.var.errorCode);
		}
	},

	telCancel : function() {
		var telNumbers = $(".number");

		for (var i=0; i<telNumbers.length; i++) {
			telNumbers.eq(i).removeClass("show").addClass("notShow");
		}

		functionality.cancel("telBox");
	},

	validateNumbers : function() {
		var numberRegEx = /^[0-9]{9}$/;
		var OK = true;

		var numbers = $(".number");
		var loopLength = $(".numbers").length;

		for (var i=0; i<loopLength; i++) {
			if (!numberRegEx.test(numbers.eq(i).val())) {
				if (numbers.eq(i).val().length < 9) {
					if (numbers.eq(i).val().length == 0) {
						functionality.var.errorCode = 0;
					}
					else {
						functionality.var.errorCode = 1;
					}
				}
				else {
					functionality.var.errorCode = 2;
				}
				OK = numberRegEx.test(numbers.eq(i).val());
				functionality.var.wrongTelNumber = i;
			}
		}

		functionality.removeNumbersError();

		return OK;
	},

	telNumberError : function(errorCode) {
		var telNumbers = $(".number");
		telNumbers.eq(functionality.var.lastWrongTelNumber)
			.removeClass("inputBorderError")
			.addClass("inputBorder");
		telNumbers.eq(functionality.var.wrongTelNumber)
			.removeClass("inputBorder")
			.addClass("inputBorderError");

		if (errorCode == 0) {
			functionality.telLength0Error();
		}
		else if (errorCode == 1) {
			functionality.telLengthError();
		}
		else {
			functionality.telCharacterError();
		}

		functionality.var.lastWrongTelNumber = functionality.var.wrongTelNumber;

		$("#telError")
			.removeClass("notShow")
			.addClass("show");
	},

	telLengthError : function() {
		document.getElementById("telErrorText").innerHTML = "Longitud insuficiente. ¡El número debe tener 9 dígitos!"
	},

	telCharacterError : function() {
		document.getElementById("telErrorText").innerHTML = "¡Has escrito algún caracter no permitido!"
	},

	telLength0Error : function() {
		document.getElementById("telErrorText").innerHTML = "¡No has escrito nada!";
	},

	removeNumbersError : function() {
		$(".number").eq(this.var.wrongTelNumber)
			.removeClass("inputBorderError")
			.addClass("inputBorder");
		$("#telError")
			.removeClass("show")
			.addClass("notShow");
	},

	stopStartService : function() {
		if (!functionality.var.stop) {
			cordova.exec(false, false, "LinkToService", "stopService", []);

			$("#stopStart").html("Arrancar detección de caídas");
			functionality.var.stop = true;
		}
		else {
			cordova.exec(false, false, "LinkToService", "reStartService", []);

			$("#stopStart").html("Parar detección de caídas");
			functionality.var.stop = false;
		}
	},

	sendAlert : function() {
		functionality.showPrompt("alertConfirmation");
	},

	alertOK : function() {
		cordova.exec(false, false, "LinkToService", "sendAlert", []);

		functionality.cancel("alertConfirmation");
	},

	cancel : function(element) {
		$("#" + element)
			.removeClass("show")
			.addClass("notShow");

		functionality.clearBackground();
	},

	darkBackground : function() {
		$("#menuLink").css("z-index", "-1");
		menuFunctions.darken(true);
	},

	clearBackground : function() {
		$("#menuLink").css("z-index", "3");
		menuFunctions.removeDarken(true);
	},

};

setEnvironment.initialize();