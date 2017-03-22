/* order list start */
(function($) {
	'use strict';
	var templateOfWarn = $("#order").html();
	var templateOfWarnDetail = $("#detail").html();
	var defaults = {
		flag : localStorage.getItem("sound") || "OPEN",// 订单提示铃声是否打开
		sound : $("#sound"),// 控制开关jquery对象
		audio : document.getElementById("audio"),// audio资源对象
		// respCode : CONSTANTS.RESP_CODE.SUCCESS,//最后一次处理订单结果
		// respDesc : "",//最后一次处理订单信息
		orders : [],// 目前已经有了的订单
		cancelURL : "json/cancel-order.json",// 取消订单URL
		acceptURL : "json/accept-order.json",//"order/loadOrder",// 接受订单URL
		saveOrderURL : "order/orderInsertDb",// 订单数据入库
		confirmOrderURL : "order/confirmOrder",
		animateIn : "fadeInDown",// zoomIn rollIn rotateIn bounceIn fadeInUp
		animateOut : "fadeOutDown"// hinge
	};
	function Plugin(options) {
		this.opt = $.extend(true, {}, defaults, options);
		this.init();
	}

	Plugin.prototype = {
		init : function() {
			this.$wol = $("#warnOrderList");// 提醒订单
			this.initAudio();// 初始化音频
			this.bindEvent();// 绑定事件
		},
		initAudio : function() {
			var opt = this.opt;
			if (opt.flag === "CLOSE") {
				opt.sound.find('.iconfont').toggleClass('hide');
			}
		},
		playAudio : function() {
			var opt = this.opt;
			if (this.opt.flag === "OPEN") {
				opt.audio.play();
			}
		},
		processOrders : function(data) {
			var that = this;
			if (data instanceof Array) {
				var f = true;
				for (var i = 0; i < data.length; i++) {
					if ($.inArray(data[i].orderNo, this.opt.orders) == -1) {
						f = false;
						break;
					}
				}
				if (f)
					return;
			}
			this.$wol.data("loading").show(function() {
						$(this).data("nodata-display").hide(true);
						$(this).data("loading").hide(function() {
									that.warnOrders(data);
								});
					});
		},
		warnOrders : function(orders) {
			if (orders instanceof Array) {
				for (var i = 0; i < orders.length; i++) {
					this.warnOrder(orders[i]);
				}
			} else {
				this.warnOrder(orders);
			}
		},
		warnOrder : function(order) {
			if ($.inArray(order.orderNo, this.opt.orders) != -1)
				return;
			this.$wol.append(this.genOrder(order, templateOfWarn,
					templateOfWarnDetail));
			this.playAudio();// 提示音播报
		},
		genOrder : function(order, temp, tempDetail) {
			var _temp = temp;
			var _details = "";
			var _regex = /(\{(.+?)\})/g;
			while(_regex.test(_temp)){
				_temp = _temp.replace(RegExp.$1, order[RegExp.$2]);
			}
			_regex = /(#\[(.+?)\])/g;
			while(_regex.test(_temp)){
				var dict = eval(RegExp.$2);
				_temp = _temp.replace(RegExp.$1,dict);
			}
			var $order = $(_temp);
			for (var i = 0; i < order.dishes.length; i++) {
				var _tempDetail = tempDetail;
				for (var prop in order.dishes[i]) {
					_tempDetail = _tempDetail.replace("\{" + prop + "\}",
							order.dishes[i][prop]);
				}
				_details += _tempDetail;
			}
			$order.find(".menu-list ul").html(_details);
			$order.addClass(this.opt.animateIn + " animated");
			$order.data("bind", order);
			this.opt.orders.push(order.orderNo);
			return $order;
		},
		hasWarnOrders : function() {
			return this.$wol.children(".order-bind").size() > 0;
		},
		orderInsertDb : function(order) {
			var that = this;
			// ajax操作
		},
		getPromise : function(url, params, callback) {
			new Promise(function(resolve, reject) {
						$.getJSON(url, params, function(res) {
									if (res.respCode === CONSTANTS.RESP_CODE.SUCCESS) {
										resolve(res);
									} else {
										reject(res);
									}
								});
					}).then(function(res) {
						callback(res);
					}, function(res) {
						console.log(res.respCode + ":" + res.respDesc);
					});
		},
		postPromise : function(url, params, callback) {
			console.log(params);
			$.ajax({
						type : "post",
						url : url,
						data : JSON.stringify(params),
						cache : false,
						dataType : "json",
						contentType : "application/json; charset=utf-8",
						error : function() {
							alert("接单失败！");
						},
						success : function(response) {
							callback(response);
						}

					});

		},
		bindEvent : function() {
			var that = this;
			var opt = this.opt;
			this.opt.sound.click(function() {
						$(this).find('.iconfont').toggleClass('hide');
						localStorage.setItem("sound",
								(opt.flag = opt.flag === "OPEN"
										? "CLOSE"
										: "OPEN"));
					});
			this.$wol.on("webkitAnimationStart", ".order-bind",
					function() {
						that.$wol.data("nodata-display").hide(true);
					});
			this.$wol.on("click", ".order-bind .btn",
					function() {
						var $order = $(this).parents(".order-bind:first");
						var bind = $order.data("bind");
						$order.removeClass(that.opt.animateIn)
							.addClass(that.opt.animateOut + ' animated')
							.one('webkitAnimationEnd', function() {
								$order.remove();
								if (!that.hasWarnOrders()) {
									that.$wol.data("nodata-display").show();
								}
								// 修改mongodb订单状态
								that.postPromise(that.opt.acceptURL, /*{
											"merchantId" : bind.merchantId,
											"orderNo" : bind.orderNo,
											"platformType" : bind.platform_type
										}*/bind, function(res) {
											if(res.respCode == '0000') {
												alert("接单成功！");
											} else {
												alert("接单失败！" + res.respDesc);
											}
										});
							});
						});
		}
	};

	$.orderList = function(options) {
		return new Plugin(options);
	};
})(window.jQuery);
/* order list end */

$(function() {
	$(".data-list").noDataDisplay({
				show : true
			});
	$(".data-list").loading();
	// var orderList = $.orderList();
	// window.setTimeout(function(){
	// // window.setInterval(function(){
	// 	$.getJSON("json/order-list.json" , function(data){
	// 		orderList.processOrders(data);
	// 	});
	// },1000);
});