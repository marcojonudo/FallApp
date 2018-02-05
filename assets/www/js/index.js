
var globalElements = {

	slider : $("#slider"),
	block : $(".block")

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
		sliderFunctions.var.h = new sliderFunctions.hashTable({"2": sliderFunctions.validateName, "3": sliderFunctions.validateNumbers, "12": sliderFunctions.nameError, "13": sliderFunctions.telNumberError, "20": sliderFunctions.nameLength0Error, "21": sliderFunctions.nameLengthError, "22": sliderFunctions.nameCharacterError, "23": sliderFunctions.telLength0Error, "24": sliderFunctions.telLengthError, "25": sliderFunctions.telCharacterError});

		setEnvironment.setListeners();
	},

	setListeners : function() {

		globalElements.slider
			.on("touchstart", sliderFunctions.touchStart)
			.on("touchmove", sliderFunctions.touchMove)
			.on("touchend", sliderFunctions.touchEnd);

		window
			.addEventListener("orientationchange", sliderFunctions.onOrientationChange);

		globalElements.block
			.on("touchstart", sliderFunctions.stopTouchEventPropagation)
			.on("touchmove", sliderFunctions.stopTouchEventPropagation)
			.on("touchend", sliderFunctions.stopTouchEventPropagation);

		$("body").on("load", sliderFunctions.setIScroll());
	}
};

var sliderFunctions = {

	elements : {
		indicator : $(".indicator"),
		back : $("#back"),
		next : $("#next"),
		userName : $("#userName"),
		nameError : $("#nameError"),
		numbers : $(".number"),
		telError : $("#telError")
	},

	var : {
		h : undefined,
		currentScreen : 0,
		slidesNumber : $(".content").length,
		pixelOffset : 0,
		bodyWidth : $("body").width(),
		wrongTelNumber : 0,
		lastWrongTelNumber : 0,
		state : "waitingTouch",
		startX : 0,
		distance : 0,
		startPixelOffset : 0,
		offsetRatio : 0,
		errorCode : 0
	},

	onOrientationChange : function() {
		if (window.innerHeight > window.innerWidth) {
			sliderFunctions.var.bodyWidth = $("body").width() * (16/9);
			sliderFunctions.var.pixelOffset = sliderFunctions.var.currentScreen * (-sliderFunctions.var.bodyWidth);
		}
		else {
			sliderFunctions.var.bodyWidth = $("body").width() / (16/9);
			sliderFunctions.var.pixelOffset = sliderFunctions.var.currentScreen * (-sliderFunctions.var.bodyWidth);
		}

		globalElements.slider
			.css("transform", "translate3d(" + sliderFunctions.var.pixelOffset + "px, 0, 0)")
			.addClass("animate");
	},

	removeNameError : function() {
		sliderFunctions.elements.userName
			.removeClass("inputBorderError")
			.addClass("inputBorder");
		sliderFunctions.elements.nameError
			.removeClass("show")
			.addClass("notShow");
	},

	removeNumbersError : function() {
		this.elements.numbers.eq(this.var.wrongTelNumber)
			.removeClass("inputBorderError")
			.addClass("inputBorder");
		this.elements.telError
			.removeClass("show")
			.addClass("notShow");
	},

	validateValues : function() {
		var OK = true;

		if (this.var.h.hasItem(this.var.currentScreen)) {
			OK = this.var.h.getItem(this.var.currentScreen)();
		}

		return OK;
	},

	validateName : function() {
		var name = $.trim(sliderFunctions.elements.userName.val())
					.replace(/\s+/g, " ")
					.split(" ");

		var nameRegEx = /^[A-ZÁÉÍÓÚÜÑ][a-záéíóúüñ]+$/;
		var correctName = true;
		for (var i=0; i<name.length; i++) {
			name[i] = name[i].charAt(0).toUpperCase() + name[i].slice(1);
			if (!nameRegEx.test(name[i])) {
				if (name[i].length < 2) {
					if (name[i].length == 0) {
						sliderFunctions.var.errorCode = 0;
					}
					else {
						sliderFunctions.var.errorCode = 1;
					}
				}
				else {
					sliderFunctions.var.errorCode = 2;
				}
				correctName = false;
			}
		}

		sliderFunctions.removeNameError();

		return correctName;
	},

	validateNumbers : function() {
		var numberRegEx = /^[0-9]{9}$/;
		var OK = true;

		sliderFunctions.elements.numbers = $(".number");

		for (var i=0; i<sliderFunctions.elements.numbers.length; i++) {
			if (!numberRegEx.test(sliderFunctions.elements.numbers.eq(i).val())) {
				if (sliderFunctions.elements.numbers.eq(i).val().length < 9) {
					if (sliderFunctions.elements.numbers.eq(i).val().length == 0) {
						sliderFunctions.var.errorCode = 3;
					}
					else {
						sliderFunctions.var.errorCode = 4;
					}
				}
				else {
					sliderFunctions.var.errorCode = 5;
				}
				OK = numberRegEx.test(sliderFunctions.elements.numbers.eq(i).val());
				sliderFunctions.var.wrongTelNumber = i;
			}
		}

		sliderFunctions.removeNumbersError();

		return OK;
	},

	nameError : function() {
		sliderFunctions.elements.userName
			.removeClass("inputBorder")
			.addClass("inputBorderError");

		sliderFunctions.informError(20 + sliderFunctions.var.errorCode);

		sliderFunctions.elements.nameError
			.removeClass("notShow")
			.addClass("show");
	},

	nameLengthError : function() {
		document.getElementById("nameErrorText").innerHTML = "Longitud insuficiente. ¡El nombre debe tener 2 letras o más!"
	},

	nameCharacterError : function() {
		document.getElementById("nameErrorText").innerHTML = "¡Has escrito algún caracter no permitido!"
	},

	nameLength0Error : function() {
		document.getElementById("nameErrorText").innerHTML = "¡No has escrito nada!";
	},

	telNumberError : function() {
		sliderFunctions.elements.numbers.eq(sliderFunctions.var.lastWrongTelNumber)
			.removeClass("inputBorderError")
			.addClass("inputBorder");
		sliderFunctions.elements.numbers.eq(sliderFunctions.var.wrongTelNumber)
			.removeClass("inputBorder")
			.addClass("inputBorderError");

		sliderFunctions.informError(20 + sliderFunctions.var.errorCode);

		sliderFunctions.var.lastWrongTelNumber = sliderFunctions.var.wrongTelNumber;

		sliderFunctions.elements.telError
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

	informError : function(error) {
		if (this.var.h.hasItem(error)) {
			this.var.h.getItem(error)();
		}
	},

	buttonsOpacityNext : function() {
		this.elements.back
			.removeClass("opacity01")
			.addClass("opacity1");

		if (this.var.currentScreen == (this.var.slidesNumber - 2)) {
			this.elements.next
				.removeClass("opacity1")
				.addClass("opacity01")
		}
	},

	buttonsOpacityBack : function() {
		this.elements.next
			.removeClass("opacity01")
			.addClass("opacity1");

		if (this.var.currentScreen == 1) {
			this.elements.back
				.removeClass("opacity1")
				.addClass("opacity01")
		}
	},

	changeIndicator : function(opacityClass) {
		this.elements.indicator.eq(this.var.currentScreen)
			.removeClass("opacity01 opacity1")
			.addClass(opacityClass);
	},

	next : function() {
		if (this.var.currentScreen != (this.var.slidesNumber-1)) {
			if (this.validateValues()) {
				this.buttonsOpacityNext();

				this.changeIndicator("opacity01");

				this.var.currentScreen++;

				this.var.pixelOffset = this.var.currentScreen * (-this.var.bodyWidth);

				globalElements.slider
					.css("transform", "translate3d(" + sliderFunctions.var.pixelOffset + "px, 0, 0)")
					.addClass("animate");

				this.changeIndicator("opacity1");
			}
			else {
				this.informError(sliderFunctions.var.currentScreen + 10);
			}
		}
	},

	back : function() {
		if (this.var.currentScreen != 0) {
			this.buttonsOpacityBack();

			this.changeIndicator("opacity01");

			this.var.currentScreen--;

			this.var.pixelOffset = this.var.currentScreen * (-this.var.bodyWidth);

			globalElements.slider
				.css("transform", "translate3d(" + sliderFunctions.var.pixelOffset + "px, 0, 0)")
				.addClass("animate");

			this.changeIndicator("opacity1");
		}

	},


	touchStart : function(event) {
		if (event.originalEvent.touches) {
			event = event.originalEvent.touches[0];
		}

		if (sliderFunctions.var.state == "waitingTouch") {
			sliderFunctions.var.startX = event.clientX;
			sliderFunctions.var.state = "startTouch";
		}
	},

	touchMove : function(event) {
		event.preventDefault();

		if (event.originalEvent.touches) {
			event = event.originalEvent.touches[0];
		}

		sliderFunctions.var.distance = event.clientX - sliderFunctions.var.startX;

		if ((sliderFunctions.var.state == "startTouch")&&(sliderFunctions.var.distance != 0)) {
			sliderFunctions.var.startPixelOffset = sliderFunctions.var.pixelOffset;
			sliderFunctions.var.state = "moveTouch";
		}

		if (sliderFunctions.var.state == "moveTouch") {
			sliderFunctions.var.offsetRatio = 1;

			if (((sliderFunctions.var.currentScreen == 0)&&(event.clientX > sliderFunctions.var.startX))
					||((sliderFunctions.var.currentScreen == sliderFunctions.var.slidesNumber - 1) &&(event.clientX < sliderFunctions.var.startX))) {
				sliderFunctions.var.offsetRatio = 3;
			}

			sliderFunctions.var.pixelOffset = sliderFunctions.var.startPixelOffset + sliderFunctions.var.distance/sliderFunctions.var.offsetRatio;

			globalElements.slider
				.css("transform", "translate3d(" + sliderFunctions.var.pixelOffset + "px, 0, 0)")
				.removeClass();
		}
	},

	touchEnd : function() {
		if (sliderFunctions.var.state == "moveTouch") {

			sliderFunctions.changeIndicator("opacity01");

			if (Math.abs((Math.abs(sliderFunctions.var.pixelOffset)-Math.abs(sliderFunctions.var.startPixelOffset)))>(sliderFunctions.var.bodyWidth/2)) {
				if (sliderFunctions.var.pixelOffset < sliderFunctions.var.startPixelOffset) {
					if (sliderFunctions.validateValues()) {
						sliderFunctions.buttonsOpacityNext();
						sliderFunctions.var.currentScreen++;
					}
					else {
						sliderFunctions.informError(sliderFunctions.var.currentScreen + 10);
					}
				}
				else {
					sliderFunctions.buttonsOpacityBack();
					sliderFunctions.var.currentScreen--;
				}
			}

			sliderFunctions.var.pixelOffset = sliderFunctions.var.currentScreen * (-sliderFunctions.var.bodyWidth);

			globalElements.slider
				.css("transform", "translate3d(" + sliderFunctions.var.pixelOffset + "px, 0, 0)")
				.removeClass()
				.addClass("animate");

			sliderFunctions.changeIndicator("opacity1");

			sliderFunctions.var.state = "waitingTouch";
		}
		else {
			sliderFunctions.var.state = "waitingTouch";
		}
	},

	stopTouchEventPropagation : function(event) {
		event.stopPropagation();
	},

	setIScroll : function() {
		new IScroll(".block");
	},


	addTelNumberBox : function() {
		sliderFunctions.elements.numbers = $(".number");

		if (this.elements.numbers.length < 5) {
			this.removeNumbersError();

			var numberInput = document.createElement("input");
			numberInput.className = "number inputBorder topMargin";
			numberInput.setAttribute("type","tel");
			numberInput.setAttribute("maxlength","9");

			$(numberInput)
				.insertAfter
					(this.elements.numbers
						[this.elements.numbers.length - 1]
					);
		}
	},

	removeTelNumberBox : function() {
		this.removeNumbersError();

		sliderFunctions.elements.numbers = $(".number");

		if (sliderFunctions.elements.numbers.length > 1) {
			var numberInput = sliderFunctions.elements.numbers[sliderFunctions.elements.numbers.length - 1];
			numberInput.parentNode.removeChild(numberInput);
		}
	},

	updateRangeText : function(value) {
		if (value<=25) {
			document.getElementById("rangeValue").innerHTML = "<strong>Valor 25 o inferior:</strong> úsalo sólo si quieres que la aplicación sea muy poco sensible. Tranquilo, te avisará si te caes de un quinto piso.";
		}
		else if (value==50) {
					document.getElementById("rangeValue").innerHTML = "<strong>Valor 50:</strong> valor por defecto, el recomendado. ¡No lo toques si no sabes lo que haces!";

		}
		else if (value>=75) {
			document.getElementById("rangeValue").innerHTML = "<strong>Valor 75 o superior:</strong> úsalo sólo si quieres que la aplicación sea muy sensible. ¡Cuidado, puede que detecte caídas donde no las hay!";
		}
	},

	finishConfiguration : function() {
		var argsJSON = this.getJSON();

		cordova.exec(false, false, "LinkToService", "startService", [argsJSON]);
	},

	getJSON : function() {
		var parametersArray = '{';
		var parameters = $("input");

		parametersArray += '"userName": "' + parameters.eq(0).val() + '", ';

		parametersArray += '"telNumbers": [';
		for (var i=1; i<parameters.length-1; i++) {
			var number = "number" + i;
			parametersArray += '{"' + number + '": "' + parameters.eq(i).val() + '"},';
		}
		parametersArray = parametersArray.slice(0, parametersArray.length - 1);
		parametersArray += '], "rangeValue": "' + parameters.eq(parameters.length - 1).val() + '"}';

		return JSON.parse(parametersArray);
	},


	hashTable : function(obj) {
		this.length = 0;
		this.items = {};
		for (var p in obj) {
			if (obj.hasOwnProperty(p)) {
				this.items[p] = obj[p];
				this.length++;
			}
		}

		this.hasItem = function(key)
		{
			return this.items.hasOwnProperty(key);
		};

		this.getItem = function(key) {
			return this.hasItem(key) ? this.items[key] : function(){};
		};
	},
};

setEnvironment.initialize();