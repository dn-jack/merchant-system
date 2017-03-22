$(function() {
	/*表单验证*/
	$('#login').bootstrapValidator().on('success.form.bv', function(e) {
        e.preventDefault();
		var form = this;
		var action = form.action;
		var usename = $('[name=username]',this).val();
		var passwords = $('[name=password]',this).val();
		//		?username="+ usename +"password="+ passwords+"
		var $loginMask = $('#mask');
		var $loginLoding = $('#loading');
		var $loginSuccess = $('#success');
		var $loginFail = $('#fail');
		$loginMask.show();
//		$.getJSON("json/login.json",function(data){
//			Utils.timeout(function(){
//				if(data.s) {
//					$loginLoding.hide();
//					$loginSuccess.show();
//					//记住密码
//					if (document.getElementById("check").checked) {
//						localStorage.setItem("rmb", true);
//						localStorage.setItem("username", usename);
//						localStorage.setItem("password", passwords);
//					}else{
//						localStorage.removeItem("rmb", null);
//						localStorage.setItem("username", usename);
//						localStorage.setItem("password", passwords);
//					}
//					Utils.timeout(function(){
//						window.location = action;
//					});
//				} else {
//					$(form).data('bootstrapValidator').resetForm(true);
//					$loginLoding.hide();
//					$loginFail.show();
//					Utils.timeout(function(){
//						$loginMask.hide();
//						$loginLoding.show();
//						$loginFail.hide();
//					});
//				}
//			});
//		});
		
					$.ajax({
						type : "post",
						url : 'http://localhost:8080/merchant-system/order/login',
						data : JSON.stringify({userName:usename,password:passwords}),
						cache : false,
						dataType : "json",
						contentType : false,
						error : function() {
							alert("登录失败！");
							$loginLoding.hide();
							$loginFail.show();
							Utils.timeout(function(){
								$loginMask.hide();
								$loginLoding.show();
								$loginFail.hide();
							});
						},
						success : function(response) {
							if(response.respCode == '0000') {
								$loginLoding.hide();
								$loginSuccess.show();
								
								var elemShops = [];
								var meituanShops = [];
								var baiduShops = [];
								
								for(var i = 0 ;i < response.result.length; i++) {
									if(response.result[i].elmId != null && response.result[i].elmId != '') {
										elemShops.push(response.result[i].elmId);
									}
									if(response.result[i].meituanId != null && response.result[i].meituanId != '') {
										meituanShops.push(response.result[i].meituanId);
									}
									if(response.result[i].baiduId != null && response.result[i].baiduId != '') {
										baiduShops.push(response.result[i].baiduId);
									}
								}
								
								var shopIds = {};
								
								
								if(elemShops.length > 0) {
									shopIds.elemShops = elemShops;
									localStorage.setItem("elemShops", elemShops);
								}
								if(meituanShops.length > 0) {
									shopIds.meituanShops = meituanShops;
									localStorage.setItem("meituanShops", meituanShops);
								}
								if(baiduShops.length > 0) {
									shopIds.baiduShops = baiduShops;
									localStorage.setItem("baiduShops", baiduShops);
								}
								localStorage.setItem("shopIds", JSON.stringify(shopIds));
								
								//记住密码
								if (document.getElementById("check").checked) {
									localStorage.setItem("rmb", true);
									localStorage.setItem("username", usename);
									localStorage.setItem("password", passwords);
								}else{
									localStorage.removeItem("rmb", null);
									localStorage.setItem("username", usename);
									localStorage.setItem("password", passwords);
								}
								Utils.timeout(function(){
									window.location = action;
								});
							} else {
								$(form).data('bootstrapValidator').resetForm(true);
								$loginLoding.hide();
								$loginFail.show();
								Utils.timeout(function(){
									$loginMask.hide();
									$loginLoding.show();
									$loginFail.hide();
								});
							}
						}
					});
	});
	var rmb = localStorage.getItem("rmb");
	if(rmb){
		//获取cookie的值
		var username = localStorage.getItem("username");
		var passwords = localStorage.getItem("username");
	　  //将获取的值填充入输入框中
		$('[name=username]').val(username);
		$('[name=password]').val(passwords);
		document.getElementById("check").checked=true;
	}
});