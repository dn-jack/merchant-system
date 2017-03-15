

$(function() {
			var orderList = $.orderList({
				acceptURL : "order/loadOrder",
				cancelURL : "order/loadOrder"
			});
			var webSocket = new WebSocket('ws://localhost:8080/merchant-system/push');

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
				var data = JSON.parse(event.data);
				if (data && data.length > 0) {
					orderList.processOrders(data);
				}
			}

			function onOpen(event) {
				// alert(event.data + '[onOpen]');
			}

			function onError(event) {
				// alert(event.data + '[onError]');
			}

			function start() {
				var param = {};
				webSocket.send('{merchantId:' + localStorage.getItem("username") +'}');
				return false;
			}
		});