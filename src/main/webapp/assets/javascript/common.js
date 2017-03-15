var CYZ = {
	name : "参与者商家中心",
	version : "0.1.0",
	desc : ""
};

var CONSTANTS = {
	EFFECT : 1000,
	RESP_CODE : {SUCCESS:"0000",ERROR:"9999"}
};

var Utils = {
	timeout : function(callback,time){
		window.setTimeout(callback,time||CONSTANTS.EFFECT);
	}
};