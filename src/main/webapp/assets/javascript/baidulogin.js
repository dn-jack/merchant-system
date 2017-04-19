$(function() {
	init();
	$('#codeImg').click(function() {
		var token = ajax("order/getToten", "");
		var src = url + '?token=' + token + '&t=' + new Date()
				+ '&color=3c78d8';
		$('#codeImg').attr('src', src);
	});

	$('#loginButton').click(function() {
				var param = {};
				param.code = $('#code').val();
				param.username = $('#username').val();
				param.password = h($('#password').val());
				ajax("order/bdlogin", param);
			});

	var webSocket = new WebSocket('wss://b.blcs.waimai.baidu.com/websocket?appid=1004&cuid=B4DE5D89D890F1745855304CE7DE5B43&osver=1.0&token=&appver=1.0&os=browser&model=pc&channel=bd');

	webSocket.onerror = function(event) {
		onError(event);
	};

	webSocket.onopen = function(event) {
		onOpen(event);
		start();
	};

	webSocket.onmessage = function(event) {
		onMessage(event);
	};

	function onMessage(event) {
		// alert(event.data + '[onMessage]');
//		var data = JSON.parse(event.data);
//		if (data && data.length > 0) {
//			orderList.processOrders(data);
//		}
		alert("onMessage");
	}

	function onOpen(event) {
		// alert(event.data + '[onOpen]');
	}

	function onError(event) {
		// alert(event.data + '[onError]');
	}

	function start() {
		var param = {};
		webSocket.send('xxxxxx');
		return false;
	}

})
var url = 'https://wmpass.baidu.com/wmpass/openservice/imgcaptcha';
var a = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
function h(r) {
	if (r) {
		r = c(r);
		var e = new RegExp("=", "g");
		return r = r.replace(e, ""), r = r.split("").reverse().join("")
	}
}

function c(r) {
	var e, t, o, c, i, h;
	for (o = r.length, t = 0, e = ""; o > t;) {
		if (c = 255 & r.charCodeAt(t++), t == o) {
			e += a.charAt(c >> 2), e += a.charAt((3 & c) << 4), e += "==";
			break
		}
		if (i = r.charCodeAt(t++), t == o) {
			e += a.charAt(c >> 2), e += a.charAt((3 & c) << 4 | (240 & i) >> 4), e += a
					.charAt((15 & i) << 2), e += "=";
			break
		}
		h = r.charCodeAt(t++), e += a.charAt(c >> 2), e += a
				.charAt((3 & c) << 4 | (240 & i) >> 4), e += a
				.charAt((15 & i) << 2 | (192 & h) >> 6), e += a.charAt(63 & h)
	}
	return e
}

function init() {
	var token = ajax("order/getToten", "");
	var src = url + '?token=' + token + '&t=' + new Date() + '&color=3c78d8';
	$('#codeImg').attr('src', src);
}
function ajax(url, param) {
	var data;
	$.ajax({
				type : "post",
				url : url,
				data : JSON.stringify(param),
				cache : false,
				async : false,
				dataType : "json",
				contentType : "application/json; charset=utf-8",
				error : function() {

				},
				success : function(response) {
					if (response.respCode == '0000') {
						data = response.token;
					}
				}
			});
	return data;
}