/* order list start */
repeat = {};
(function($) {
	'use strict';
	var templateOfWarn = $("#order").html();
	var templateOfWarnDetail = $("#detail").html();
	var defaults = {
		AUTO_ACCEPT_TIME:4000,
		flag : localStorage.getItem("sound") || "OPEN",// 订单提示铃声是否打开
		auto : localStorage.getItem("auto") || "OPEN",// 自动接单是否打开
		sound : $("#sound"),// 控制开关jquery对象
		autoBtn : $("#lcs_check"),
		audio : document.getElementById("audio"),// audio资源对象
		// respCode : CONSTANTS.RESP_CODE.SUCCESS,//最后一次处理订单结果
		// respDesc : "",//最后一次处理订单信息
		orders : [],// 目前已经有了的订单
		cancelURL : "json/cancel-order.json",// 取消订单URL
		acceptURL : "order/loadOrder",// 接受订单URL
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
			this.initAuto();
			this.bindEvent();// 绑定事件
		},
		initAudio : function() {
			var opt = this.opt;
			if (opt.flag === "CLOSE") {
				opt.sound.find('.iconfont').toggleClass('hide');
			}
		},
		initAuto : function() {
			var opt = this.opt;
			if (opt.auto === "OPEN") {
				opt.autoBtn.lcs_on();
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
		contrastOrders: function(data,platform){
			var that = this;
			this.warnOrders(data);
			$("[platform="+ platform +"]").each(function(){
				var _this = this;
				if(data.every(function(item, qindex, arrays){   //历史订单数据数据新增订单删除
					return $(_this).data("bind").orderNo !== item.orderNo;
				})) {
					that.removeOrders($(_this));
				}
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
			this.$wol.prepend(this.genOrder(order, templateOfWarn,
					templateOfWarnDetail));
			this.playAudio();// 提示音播报
		},
		genOrder : function(order, temp, tempDetail) {
			var _temp = temp;
			var _details = "";
			var _regex = /(\{(.+?)\})/;
			while(_regex.exec(_temp)){
				_temp = _temp.replace(RegExp.$1, order[RegExp.$2]);
			}
			_regex = /(#\[(.+?)\])/;
			while(_regex.exec(_temp)){
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
			$("[data-if]",$order).each(function(){
				if(!order[$(this).data("if")]){
					$(this).remove();
				}
			});
			window.setTimeout(function(){
				if($("#lcs_check").is(":checked")){
					$order.find(".btn").trigger("click");
				}
			},this.opt.AUTO_ACCEPT_TIME);
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
		removeOrders: function($order){
			$order.removeClass(this.opt.animateIn)
				.addClass(this.opt.animateOut + ' animated')
				.one('webkitAnimationEnd', function() {
					$order.remove();
					if (!this.hasWarnOrders()) {
						this.$wol.data("nodata-display").show();
					}
				});
		},
		postPromise : function(url, params, callback) {
			console.log(params);
			$.ajax({
						type : "post",
						url : url,
						data : JSON.stringify(params),
						cache : false,
						async : false,
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
			this.opt.autoBtn.on("lcs-statuschange", function(){
				localStorage.setItem("auto",$(this).is(':checked')?"OPEN":"CLOSE");
			});
			this.$wol.on("webkitAnimationStart", ".order-bind",
					function() {
						that.$wol.data("nodata-display").hide(true);
					});
			this.$wol.on("click", ".order-bind .btn",
					function() {
						console.log($(this));
						var $order = $(this).parents(".order-bind:first");
						var bind = $order.data("bind");
//						bind.channelType = 'PC';
						$order.removeClass(that.opt.animateIn)
							.addClass(that.opt.animateOut + ' animated')
							.one('webkitAnimationEnd', function() {
								$order.remove();
								if (!that.hasWarnOrders()) {
									that.$wol.data("nodata-display").show();
								}
								if(!repeat[bind.orderNo]) {
									// 修改mongodb订单状态
									that.postPromise(that.opt.acceptURL,
											bind, function(res) {
												if(res.respCode == '0000') {
													repeat[bind.orderNo] = bind.orderNo;
													alert("接单成功！");
												} else {
													alert("接单失败！");
												}
											});
								}
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
	
	$("#userName").html(localStorage.getItem("username"));
	
	$(".data-list").noDataDisplay({
				show : true
			});
	$(".data-list").loading();
	orderList = $.orderList();
	//界面初始化的时候调用一次加载订单
	queryOrder();
	//每隔30秒调用一次加载订单
	window.setInterval(queryOrder, 30000);
	
	function queryOrder(){
		var elemShops = localStorage.getItem("elemShops");
		var meituanShops = localStorage.getItem("meituanShops");
		var baiduShops = localStorage.getItem("baiduShops");
		
		if(elemShops) {
			elemShops = JSON.parse(elemShops);
			for(var i = 0 ;i<elemShops.length;i++) {
				var param = {};
				param.username = elemShops[i].elmUsername;
				param.password = elemShops[i].elmPwd;
				param.shopId = elemShops[i].shopId;
				$.ajax({
					type : "post",
					url : "query/elmQueryOrder",
					data : JSON.stringify(param),
					cache : false,
					dataType : "json",
					contentType : "application/json; charset=utf-8",
					error : function() {
						alert("查询订单失败！");
					},
					success : function(response) {
						if("0000" == response.respCode) {
							orderList.contrastOrders(response.result,'elm');
						}
					}
				});
			}
		}
		if(meituanShops) {
			meituanShops = JSON.parse(meituanShops);
			for(var i = 0 ;i<meituanShops.length;i++) {
				var param = {};
				param.username = meituanShops[i].meituanId;
				param.password = meituanShops[i].meituanPwd;
				$.ajax({
					type : "post",
					url : "query/mtQueryOrder",
					data : JSON.stringify(param),
					cache : false,
					dataType : "json",
					contentType : "application/json; charset=utf-8",
					error : function() {
						alert("查询订单失败！");
					},
					success : function(response) {
						if("0000" == response.respCode) {
							orderList.contrastOrders(response.result,'mt');
						}
					}
				});
			}
		}
		if(baiduShops) {
			baiduShops = JSON.parse(baiduShops);
			for(var i = 0 ;i<baiduShops.length;i++) {
				var param = {};
				param.username = baiduShops[i].baiduId;
				param.password = baiduShops[i].baidupwd;
				$.ajax({
					type : "post",
					url : "query/bdwmQueryOrder",
					data : JSON.stringify(param),
					cache : false,
					dataType : "json",
					contentType : "application/json; charset=utf-8",
					error : function() {
						alert("查询订单失败！");
					},
					success : function(response) {
						if("0000" == response.respCode) {
							orderList.contrastOrders(response.result,'bdwm');
						}
					}
				});
			}
		}
	}
	
	function ajax(url,params) {
		var result;
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
				result = response;
			}

		});
		return result;
	}
});