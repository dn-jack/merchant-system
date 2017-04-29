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
	},
	getDict : function(dict,key){
		return DICTS[dict][key]||"";
	}
};

var DICTS = {
	PLATFORM : {
		"elm" : "饿了么",
		"mt" : "美团",
		"bdwm" : "百度外卖"
	},
	ORDER_STATUS : {
		"0" : "等待配送",
		"1" : "正在配送",
		"2" : "订单完成"
	}
};

var SYSTEM_PLANTFORM = navigator.userAgent;
var isAndroid = SYSTEM_PLANTFORM.indexOf('Android') > -1 || SYSTEM_PLANTFORM.indexOf('Adr') > -1; //android终端
