/*left menu start*/
(function ($) {
  'use strict';
  var defaults = {};
  function Sidenav (element, options) {
    this.$el = $(element);
    this.opt = $.extend(true, {}, defaults, options);
    this.init(this);
  }

  Sidenav.prototype = {
    init: function (self) {
      self.slideTag(self);
    },
    slideTag: function(self){
        self.$el.find('li').on('click',function(){
            $(this).children("a").addClass("index").parent().siblings().children("a").removeClass("index");
            $(this).children("ul").slideToggle(100);
            if($(this).parent().hasClass("sidenav-menu")){
                localStorage.setItem("pmenu", $(this).index());
                localStorage.setItem("cmenu", undefined);
            }else{
                localStorage.setItem("cmenu", $(this).index());
            }
            var href = $(this).children("a").data("href");
            if(href){
                window.location.href = href;
            }
            return false;
        });
        self.$el.find('.memMenuTitle').click(function(){
            var state = [ 'index', 0];
            for(var i = 0; i<state.length; i++) {
                state[i] = 0;
            }
            //console.log(self.$el.find('.memMenuTitle'))
            state[$(this).index()] = 'index';

            localStorage.setItem("site", state);
        })
    }
  };

  $.fn.sidenav = function (options) {
    return this.each(function() {
      if (!$.data(this, 'sidenav')) {
        $.data(this, 'sidenav', new Sidenav(this, options));
      }
    });
  };
    var pm = localStorage.getItem("pmenu");
    var cm = localStorage.getItem("cmenu");
    if(pm!=undefined){
        var pmd = $(".sidenav-menu li").eq(pm);
        pmd.children("a").addClass("index").next().show();
        if(cm!=undefined){
            $(".sidenav-dropdown li",pmd).eq(cm).children("a").addClass("index");
        }
    }
})(window.jQuery);


/*left menu end*/

/*drop down start*/
(function ($) {
    'use strict';
    var defaults = {};
    function Plugin (element, options) {
        this.$el = element;
        this.opt = $.extend(true, {}, defaults, options);
        this.init(this);
    }

    Plugin.prototype = {
        init: function(self){
            self.initToggle(self);
        },
        initToggle: function(self){
            $('button[dropDown-handle]').on('click', function(){
                $(this).toggleClass('index');
                $(this).parents('.handle').prevAll('.checked-menu').slideToggle('fast');
            })
        }
    };

    $.fn.dropdown = function (options) {
      return this.each(function() {
        if (!$.data(this, 'dropdown')) {
          $.data(this, 'dropdown', new Plugin(this, options));
        }
      });
    };
})(window.jQuery);
/*drop down end*/

/*no data display start*/
(function ($) {
    'use strict';
    var defaults = {
        show : false,
        text : "暂时没有新增订单...",
        img : "assets/images/null.svg",
        animateIn : "bounceIn",//bounceIn fadeIn
        animateOut : "bounceOut"//bounceOut fadeOut
    };
    var template = '<div class="nodata-display">'+
                        '<img src="{img}">'+
                        '<h5>{text}</h5>'+
                    '</div>';
    function Plugin (element, options) {
        this.$el = $(element);
        this.opt = $.extend(true, {}, defaults, options);
        this.init();
    }

    Plugin.prototype = {
        init: function(){
            var _temp = template;
            for(var prop in this.opt){
                _temp = _temp.replace("\{"+prop+"\}",this.opt[prop]);
            }
            this.$ndd = $(_temp);
            this.$el.append(this.$ndd);
            this.opt.show&&this.show();
        },
        show: function(){
            var that = this;
            var animateIn = this.opt.animateIn;
            this.$ndd.off("webkitAnimationEnd").removeClass().addClass(animateIn + ' animated nodata-display').show();
        },
        hide: function(callback){
            if(typeof callback=='boolean'){
                this.$ndd.hide();
                return;
            }
            var animateOut = this.opt.animateOut;
            var that = this;
            this.$ndd.removeClass().addClass(animateOut + ' animated nodata-display').one('webkitAnimationEnd', function(){
              $(this).hide();
              callback&&callback.call(that);
            });
        }
    };

    $.fn.noDataDisplay = function (options) {
      return this.each(function() {
        if (!$.data(this, 'nodata-display')) {
          $.data(this, 'nodata-display', new Plugin(this, options));
        }
      });
    };
})(window.jQuery);
/*no data display end*/

/*loading start*/
(function ($) {
    'use strict';
    var defaults = {
        show : false,
        img : "assets/images/loading.svg",
        animateIn : "bounceIn",//bounceIn
        animateOut : "bounceOut"
    };
    var template = '<div class="loading">'+
                    '<img src="{img}">'+
                   '</div>';
    function Plugin (element, options) {
        this.$el = $(element);
        this.opt = $.extend(true, {}, defaults, options);
        this.init();
    }

    Plugin.prototype = {
        init: function(){
            var _temp = template;
            for(var prop in this.opt){
                _temp = _temp.replace("\{"+prop+"\}",this.opt[prop]);
            }
            this.$ndd = $(_temp);
            this.$el.append(this.$ndd);
            this.opt.show&&this.show();
        },
        show: function(callback){
            var animateIn = this.opt.animateIn;
            this.$ndd.off("webkitAnimationEnd").removeClass().addClass(animateIn + ' animated loading');
            this.$el.append(this.$ndd);
            this.$ndd.show();
            callback&&callback.call(this.$el);
        },
        hide: function(callback){
            var animateOut = this.opt.animateOut;
            var that = this;
            var $ndd = this.$ndd;
            window.setTimeout(function(){
                $ndd.removeClass().addClass(animateOut + ' animated loading').one('webkitAnimationEnd', function(){
                  $(this).hide();
                  callback&&callback.call(that.$el);
                });
            },500);
        }
    };

    $.fn.loading = function (options) {
      return this.each(function() {
        if (!$.data(this, 'loading')) {
          $.data(this, 'loading', new Plugin(this, options));
        }
      });
    };
})(window.jQuery);
/*loading end*/

$(function(){
    $("#menu").sidenav();
});