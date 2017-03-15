/* order list start */
(function($) {
	'use strict';
	var templateOfWarn = '<div class="container-fluid order-list ">                                                              '
			+ '    <div class="order-title new-order-title">                                                         '
			+ '        <h1>{userName} <span>{phone}</span> <a href="javascript:void(0);"><i class="iconfont">&#xe737;</i></a></h1>    '
			+ '        <h4><label class="meituan" for="">【美团外卖】</label><span>{orderTime}</span></h4>      '
			+ '    </div>                                                                                            '
			+ '    <ul>                                                                                              '
			+ '        <li class="dishes-list">                                                                      '
			+ '            <table class="order-details">                                                                          '
			+ '            </table>                                                                                  '
			+ '        </li>                                                                                         '
			+ '        <li class="other-cost list-module">                                                           '
			+ '            <ul>                                                                                      '
			+ '                <li><p>餐盒费</p><span>{boxPrice}</span></li>                                               '
			+ '                <li><p>配送费</p><span>{distributionPrice}</span></li>                                               '
			+ '            </ul>                                                                                     '
			+ '        </li>                                                                                         '
			+ '        <li class="activity list-module">                                                             '
			+ '            <ul>                                                                                      '
			+ '                <li><p>立减优惠</p><span>-{discount}</span></li>                                            '
			+ '                <li><p>红包抵扣</p><span>-{hongbao}</span></li>                                            '
			+ '            </ul>                                                                                     '
			+ '        </li>                                                                                         '
			+ '        <li class="sum list-module">                                                                  '
			+ '            <ul>                                                                                      '
			+ '                <li><p>订单金额</p><span>{orderPrice}</span></li>                                           '
			+ '            </ul>                                                                                     '
			+ '        </li>                                                                                         '
			+ '    </ul>                                                                                             '
			+ '    <div class="">                                                                                    '
			+ '        <button type="button" class="btn btn-theme-yellow">接受订单</button>                          '
			+ '    </div>                                                                                            '
			+ '</div>                                                                                                ';
	var templateOfWarnDetail = '<tr>'
			+ '<td>{dishName}<span class="label label-danger">{activityName}</span></td>'
			+ '<td>×{count}</td>'
			+ '<td><label style="text-decoration:line-through"></label> <label>{price2}</label></td>'
			+ '</tr>';
	var templateOfNew = '<div class="container-fluid order-list">                                               '
			+ '    <div class="order-title">                                                          '
			+ '        <div class="order-time">                                                       '
			+ '            <span>下单时间:</span> {orderTime}                                    '
			+ '        </div>                                                                         '
			+ '        <div class="platform">                                                         '
			+ '            【美团外卖】                                                               '
			+ '        </div>                                                                         '
			+ '        <div class="order-infor">                                                      '
			+ '            <span>订单号:</span>                                                       '
			+ '            {orderNo}                                                       '
			+ '        </div>                                                                         '
			+ '        <div class="user">                                                             '
			+ '            {userName} {sex}                                                                '
			+ '        </div>                                                                         '
			+ '        <div class="phone-number">                                                     '
			+ '            <span>联系方式:</span> {phone}                                       '
			+ '        </div>                                                                         '
			+ '        <span class="label label-success">已付款</span>                                '
			+ '    </div>                                                                             '
			+ '    <ul>                                                                               '
			+ '        <li class="dishes-list">                                                       '
			+ '            <table class="order-details">                                                                '
			+ '            </table>                                                                   '
			+ '        </li>                                                                          '
			+ '        <li class="other-cost list-module">                                            '
			+ '            <ul>                                                                       '
			+ '                <li><p>餐盒费</p><span>{boxPrice}</span></li>                                '
			+ '                <li><p>配送费</p><span>{distributionPrice}</span></li>                                '
			+ '            </ul>                                                                      '
			+ '        </li>                                                                          '
			+ '        <li class="activity list-module">                                              '
			+ '            <ul>                                                                       '
			+ '                <li><p>立减优惠</p><span>-{discount}</span></li>                             '
			+ '                <li><p>红包抵扣</p><span>-{hongbao}</span></li>                             '
			+ '            </ul>                                                                      '
			+ '        </li>                                                                          '
			+ '        <li class="sum list-module">                                                   '
			+ '            <ul>                                                                       '
			+ '                <li><p>订单金额</p><span>{orderPrice}</span></li>                            '
			+ '            </ul>                                                                      '
			+ '        </li>                                                                          '
			+ '    </ul>                                                                              '
			+ '</div>                                                                                 ';
	var templateOfNewDetail = '<tr>'
			+ '<td>{dishName} <span class="label label-warning">{activityName}</span></td>'
			+ '<td>×{count}</td>'
			+ '<td><label style="text-decoration:line-through">{price1}</label></td>'
			+ '<td>{price2}</td>' + '</tr>';
	var defaults = {
		flag : localStorage.getItem("sound") || "OPEN",// 订单提示铃声是否打开
		sound : $("#sound"),// 控制开关jquery对象
		audio : document.getElementById("audio"),// audio资源对象
		// respCode : CONSTANTS.RESP_CODE.SUCCESS,//最后一次处理订单结果
		// respDesc : "",//最后一次处理订单信息
		orders : [],// 目前已经有了的订单
		cancelURL : "json/cancel-order.json",// 取消订单URL
		acceptURL : "order/loadOrder",// 接受订单URL
		saveOrderURL : "order/orderInsertDb",// 订单数据入库
		confirmOrderURL : "order/confirmOrder",
		animateIn : "rotateIn",// zoomIn rollIn rotateIn bounceIn fadeInUp
		animateOut : "rotateOut"// hinge
	};
	function Plugin(options) {
		this.opt = $.extend(true, {}, defaults, options);
		this.init();
	}

	Plugin.prototype = {
		init : function() {
			this.$nol = $("#newOrderList");// 最新订单
			this.$wol = $("#warnOrderList");// 提醒订单
			this.initAudio();// 初始化音频
			this.bindEvent();// 绑定事件
		},
		initAudio : function() {
			var opt = this.opt;
			if (opt.flag === "CLOSE") {
				$("#sound").find('.iconfont').toggleClass('hide');
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
			for (var prop in order) {
				_temp = _temp.replace("\{" + prop + "\}", order[prop]);
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
			$order.find(".order-details").html(_details);
			$order.addClass(this.opt.animateIn + " animated");
			$order.data("bind", order);
			this.opt.orders.push(order.orderNo);
			return $order;
		},
		hasWarnOrders : function() {
			return this.$wol.children(".order-list").size() > 0;
		},
		newOrder : function(order) {
			var that = this;
			// ajax操作
			that.$nol.append(that.genOrder(order, templateOfNew,
					templateOfNewDetail));
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
							alert("订单数据保存失败！");
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
			this.$wol.on("webkitAnimationStart", ".container-fluid.order-list",
					function() {
						that.$wol.data("nodata-display").hide(true);
					});
			this.$wol.on("click", ".container-fluid.order-list .btn",
					function() {
						var $order = $(this).parents(".order-list:first");
						var bind = $order.data("bind");
						that.$nol.data("nodata-display").hide();
						$order.removeClass(that.opt.animateIn)
								.addClass(that.opt.animateOut + ' animated')
								.one('webkitAnimationEnd', function() {
									$order.remove();
									if (!that.hasWarnOrders()) {
										that.$wol.data("nodata-display").show();
									}
									// 修改mongodb订单状态
									that.getPromise(that.opt.acceptURL, {
												"merchantId" : bind.merchantId,
												"orderNo" : bind.orderNo
											}, function(res) {
												that.newOrder(bind);
											});
									// 把订单数据插入数据库
									that.postPromise(that.opt.saveOrderURL,
											bind, function(res) {
												if (res.respCode == '0000') {
													// 调用饿了么订单确认接口
													that
															.getPromise(
																	that.opt.confirmOrderURL,
																	{
																		"merchantId" : bind.merchantId,
																		"orderNo" : bind.orderNo
																	},
																	function(
																			res) {
																		if (res.respCode == '0000') {
																			alert("接单成功！");
																		} else {
																			alert("接单失败！"
																					+ res.respDesc);
																			return;
																		}
																	});
												} else {
													alert("接单失败！"
															+ res.respDesc);
													return;
												}
											});

								});
					}).on("click", ".container-fluid .new-order-title h1 a i",
					function() {
						var $order = $(this).parents(".order-list:first");
						var bind = $order.data("bind");
						$order.removeClass(that.opt.animateIn)
								.addClass(that.opt.animateOut + ' animated')
								.one('webkitAnimationEnd', function() {
									$order.remove();
									if (!that.hasWarnOrders()) {
										that.$wol.data("nodata-display").show();
									}
									that.getPromise(that.opt.cancelURL, {
												"merchantId" : bind.merchantId,
												"orderNo" : bind.orderNo
											}, function(res) {

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
		// window.setInterval(function(){
		// $.getJSON("json/order-list.json" , function(data){
		// orderList.processOrders(data);
		// });
		// },5000);
	});