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
		$.getJSON("json/login.json",function(data){
			Utils.timeout(function(){
				if(data.s) {
					$loginLoding.hide();
					$loginSuccess.show();
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
			});
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