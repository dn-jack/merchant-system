/* order list start */
(function($) {
	'use strict';
	var templateOfOrder = $("#order").html();
	var templateOfOrderDetail = $("#detail").html();
	
	var date = new Date();
    var seperator1 = "-";
    var month = date.getMonth() + 1;
    var strDate = date.getDate();
    if (month >= 1 && month <= 9) {
        month = "0" + month;
    }
    if (strDate >= 0 && strDate <= 9) {
        strDate = "0" + strDate;
    }
	var currentdate = date.getFullYear() + seperator1 + month + seperator1 + strDate;
	var defaults = {
		queryURL : "order/searchOrder",//查询地址
		querys : {
			'platformType':'all',
			'orderLatestStatus' : 'all',
			'beginTime' : currentdate
		},//查询条件
		animateIn : "fadeInDown",// zoomIn rollIn rotateIn bounceIn fadeInUp
		animateOut : "fadeOutDown"// hinge
	};
	function Plugin(options) {
		this.opt = $.extend(true, {}, defaults, options);
		this.init();
	}

	Plugin.prototype = {
		init : function() {
			this.$osl = $("#order-search");// 订单
			this.$ool = this.$osl.find("ul");
			this.bindEvent();// 绑定事件
		},
		processOrders : function(data) {
			var that = this;
			this.$osl.data("loading").show(function() {
				$(this).data("nodata-display").hide(true);
				$(this).data("loading").hide(function() {
					that.addOrders(data);
				});
			});
		},
		addOrders : function(orders) {
			if (orders instanceof Array) {
				for (var i = 0; i < orders.length; i++) {
					orders[i].serial = i+1;
					this.addOrder(orders[i]);
				}
			} else {
				this.addOrder(orders);
			}
		},
		addOrder : function(order) {
			this.$ool.append(this.genOrder(order, templateOfOrder,
					templateOfOrderDetail));
		},
		genOrder : function(order, temp, tempDetail) {
			order.serial = order.serial||1;
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
			$order.find(".menu-list").html(_details);
			$order.addClass(this.opt.animateIn + " animated");
			$order.data("bind", order);
			return $order;
		},
		hasOrders : function() {
			return this.$ool.children("li").size() > 0;
		},
		emptyOrders : function(){
			this.$ool.empty();
		},
		load : function(){
			var that = this;
			console.log(JSON.stringify(this.opt.querys));
			this.opt.querys.shopIds = localStorage.getItem("shopIds");
			$.ajax({
				type : "post",
				url : this.opt.queryURL,
				data : JSON.stringify(this.opt.querys),
				cache : false,
				dataType : "json",
				contentType : "application/json; charset=utf-8",
				error : function() {
					alert("订单数据查询失败！");
				},
				success : function(data) {
					if(data.respCode == '0000') {
						that.emptyOrders();
						that.processOrders(data.result);
					} else {
						alert("订单数据查询失败！");
					}
				}

			});
		},
		addQuery : function(name,value){
			this.opt.querys[name] = value;
		},
		bindEvent : function() {
			var that = this;
			var opt = this.opt;
			this.$ool.on("webkitAnimationStart", ".order-search ul li",
					function() {
						that.$ool.data("nodata-display").hide(true);
					});
		}
	};

	$.orderList = function(options) {
		return new Plugin(options);
	};
})(window.jQuery);
/* order list end */

$(function() {
	var orderList = $.orderList();
	window.setTimeout(function(){
		orderList.load();
	},1000);
	$(".data-list").noDataDisplay({
				show : true
			});
	$(".data-list").loading();
	$("#menu").sidenav();
    $('#order-search').dropdown();
   	 $('input').iCheck({
        radioClass: 'iradio_square-red',
        increaseArea: '20%' // optional
    });
    var $dateForm = $(".date-form");
	var $dt  = $('#datetimepicker');
	$dt.datetimepicker({
        language: 'zh',
        weekStart: 1,
        todayBtn:  1,
        autoclose: 1,
        todayHighlight: 1,
        startView: 2,
        minView: 2,
        forceParse: 0
    });
    
    $dt.datetimepicker().on('changeDate', function(ev){
	    orderList.addQuery('beginTime',ev.date.valueOf());
	});
	$("input[name='iCheck1']").on("ifUnchecked", function(){
		if($(this).attr("id") == "radio-5") {
			$dateForm.css("visibility", "visible");
		} else {
			$dateForm.css("visibility", "hidden");
		}
	});	
	$(".filter input[type=radio]").on("ifChecked", function(){
		orderList.addQuery($(this).attr("name"),$(this).val());
		orderList.load();
	});	
	
});