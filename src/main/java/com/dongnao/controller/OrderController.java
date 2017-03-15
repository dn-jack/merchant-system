package com.dongnao.controller;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dongnao.service.impl.OrderService;
import com.dongnao.util.ElemeUtil;
import com.dongnao.util.HttpsRequestUtil;
import com.dongnao.util.JsonUtil;
import com.dongnao.util.SpringContextHolder;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/** 
 * @Description TODO 
 * @ClassName   OrderController 
 * @Date        2017年2月10日 下午5:19:57 
 * @Author      luoyang 
 */
@Controller
@RequestMapping("/order")
public class OrderController {
    
    private static transient Logger log = LoggerFactory.getLogger(OrderController.class);
    
    @Autowired
    @Qualifier("mongoTemplate")
    MongoTemplate mt;
    
    @Autowired
    OrderService os;
    
    @RequestMapping("/loadOrder")
    public @ResponseBody String loadOrder(HttpServletRequest request,
            @RequestBody String param) {
        
        JSONObject paramJo = JSON.parseObject(param);
        
        DBCollection dbc = mt.getCollection("dn_order");
        BasicDBObject cond2 = new BasicDBObject();
        cond2.put("merchantId", request.getParameter("merchantId"));
        cond2.put("orderNo", request.getParameter("orderNo"));
        cond2.put("state", "0");
        
        BasicDBObject cond3 = new BasicDBObject();
        BasicDBObject cond4 = new BasicDBObject();
        cond4.put("state", "1");
        cond3.put("$set", cond4);
        dbc.updateMulti(cond2, cond3);
        
        JSONObject retJo = new JSONObject();
        retJo.put("respCode", "0000");
        retJo.put("respDesc", "成功！");
        return retJo.toJSONString();
    }
    
    @RequestMapping("/orderInsertDb")
    public @ResponseBody String orderInsertDb(HttpServletRequest request,
            @RequestBody String param) {
        try {
            return os.saveOrder(param);
        }
        catch (Exception e) {
            e.printStackTrace();
            JSONObject reJo = new JSONObject();
            reJo.put("respCode", "9999");
            reJo.put("respDesc", e.getMessage());
            return reJo.toString();
        }
    }
    
    @RequestMapping("/confirmOrder")
    public @ResponseBody String confirmOrder(HttpServletRequest request,
            @RequestBody String param) {
        
        MongoTemplate mt = (MongoTemplate)SpringContextHolder.getWebApplicationContext()
                .getBean("mongoTemplate");
        
        DBCollection dbc = mt.getCollection("dn_ksid");
        BasicDBObject cond1 = new BasicDBObject();
        cond1.put("shopId", request.getParameter("merchantId"));
        DBCursor cursor = dbc.find(cond1);
        
        String ksid = null;
        
        while (cursor.hasNext()) {
            DBObject dbo = cursor.next();
            ksid = dbo.get("ksid").toString();
        }
        
        JSONObject confirmOrderJo = ElemeUtil.confirmOrderJo();
        confirmOrderJo.getJSONObject("metas").put("ksid", ksid);
        confirmOrderJo.getJSONObject("params").put("orderId",
                request.getParameter("orderNo"));
        
        try {
            String confirmOrderRe = HttpsRequestUtil.doPost(ElemeUtil.confirmOrderUrl,
                    confirmOrderJo.toString(),
                    "UTF-8",
                    300000,
                    300000);
            
            log.info("--------------confirmOrderUrl-----------------"
                    + confirmOrderRe);
            JSONObject cor = JSON.parseObject(confirmOrderRe);
            if (JsonUtil.isNotBlank(cor.get("error"))) {
                JSONObject reJo = new JSONObject();
                reJo.put("respCode", "9999");
                reJo.put("respDesc", "接单失败！");
                return reJo.toString();
            }
            
            JSONObject retJo = new JSONObject();
            retJo.put("respCode", "0000");
            retJo.put("respDesc", "成功！");
            return retJo.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
            JSONObject reJo = new JSONObject();
            reJo.put("respCode", "9999");
            reJo.put("respDesc", e.getMessage());
            return reJo.toString();
        }
        
    }
    //    @RequestMapping("/testOrder")
    //    public @ResponseBody String test(HttpServletRequest request,
    //            HttpServletResponse response) {
    //        String joStr = "{"+
    //    "'id': '1f0f702e-17c1-4e90-bc21-b1706b9f867b',"+
    //    "'ncp': '2.0.0',"+
    //    "'result': ["+
    //        "{"+
    //            "'activeTime': '2017-03-02T16:40:17',"+
    //            "'activities': ["+
    //                "{"+
    //                    "'amount': -20.0,"+
    //                    "'categoryId': 12,"+
    //                    "'elemePart': 0.0,"+
    //                    "'name': '(不与美食活动同享)在线支付立减优惠',"+
    //                    "'restaurantPart': 0.0"+
    //                "}"+
    //            "],"+
    //            "'activityTotal': -20.0,"+
    //            "'anonymousOrder': false,"+
    //            "'bookedTime': '2017-03-02T17:45:00',"+
    //            "'callDeliveryType': 'DISABLE',"+
    //            "'consigneeAddress': '长远大厦风火林网咖22号机',"+
    //            "'consigneeName': '徐明昊 先生',"+
    //            "'consigneePhones': ["+
    //                "'13365853748'"+
    //            "],"+
    //            "'consigneeSecretPhones': ["+
    //                "'13365853748'"+
    //            "],"+
    //            "'customerActualFee': 9.0,"+
    //            "'daySn': 5,"+
    //            "'deliveryCost': 9.0,"+
    //            "'deliveryFee': 9.0,"+
    //            "'deliveryFeeTotal': 9.0,"+
    //            "'deliveryServiceType': 'CROWD',"+
    //            "'distance': '1.93km',"+
    //            "'downgraded': false,"+
    //            "'elemeActivityPart': -14.0,"+
    //            "'elemeActivityPartPositive': 14.0,"+
    //            "'elemeMerchantSubsidy': 0.0,"+
    //            "'elemePart': -14.0,"+
    //            "'expectDeliveryCost': 0.0,"+
    //            "'expectDeliveryCostDetail': [],"+
    //            "'expectDeliveryCostForDetail': 0.0,"+
    //            "'feedbackStatus': 'WAITING',"+
    //            "'followed': false,"+
    //            "'goodsSummary': '共 3 件商品',"+
    //            "'goodsTotal': 53.0,"+
    //            "'goodsTotalWithoutPackage': 50.0,"+
    //            "'groups': ["+
    //                "{"+
    //                    "'items': ["+
    //                        "{"+
    //                            "'additions': [],"+
    //                            "'categoryId': 1,"+
    //                            "'discount': 0.0,"+
    //                            "'displayQuantity': true,"+
    //                            "'id': 155839050,"+
    //                            "'name': '掌中宝',"+
    //                            "'price': 20.0,"+
    //                            "'quantity': 1,"+
    //                            "'skuId': 103099203356,"+
    //                            "'total': 20.0"+
    //                        "},"+
    //                        "{"+
    //                            "'additions': [],"+
    //                            "'categoryId': 1,"+
    //                            "'discount': 0.0,"+
    //                            "'displayQuantity': true,"+
    //                            "'id': 509416883,"+
    //                            "'name': '富得流油（牛油）',"+
    //                            "'price': 20.0,"+
    //                            "'quantity': 1,"+
    //                            "'skuId': 122002888476,"+
    //                            "'total': 20.0"+
    //                        },
    //                        {
    //                            'additions': [],
    //                            'categoryId': 1,
    //                            'discount': 0.0,
    //                            'displayQuantity': true,
    //                            'id': 170745348,
    //                            'name': '猿粉',
    //                            'price': 10.0,
    //                            'quantity': 1,
    //                            'skuId': 103099223836,
    //                            'total': 10.0
    //                        }
    //                    ],
    //                    'name': '1号篮子',
    //                    'type': 'NORMAL'
    //                },
    //                {
    //                    'items': [
    //                        {
    //                            'additions': [],
    //                            'categoryId': 102,
    //                            'discount': 0.0,
    //                            'displayQuantity': false,
    //                            'id': -70000,
    //                            'name': '餐盒',
    //                            'price': 3.0,
    //                            'quantity': 1,
    //                            'skuId': -1,
    //                            'total': 3.0
    //                        }
    //                    ],
    //                    'name': '其它费用',
    //                    'type': 'EXTRA'
    //                }
    //            ],
    //            'headPromptForApp': [
    //                {
    //                    'bgColor': '#424242',
    //                    'fgColor': '#FFFFFF',
    //                    'text': '[预]'
    //                },
    //                {
    //                    'bgColor': '#424242',
    //                    'fgColor': '#FF9800',
    //                    'text': ' 今日 17:45 '
    //                },
    //                {
    //                    'bgColor': '#424242',
    //                    'fgColor': '#FFFFFF',
    //                    'text': '送达'
    //                }
    //            ],
    //            'headPromptForPC': '<font color='#333333' bgcolor='#FFFFFF'>[预]<font color='#FF6D00'> 今日 17:45 </font>送达</font>',
    //            'hongbao': 0.0,
    //            'id': '1202683964618519751',
    //            'income': 53.65,
    //            'merchantActivities': [
    //                {
    //                    'amount': -20.0,
    //                    'categoryId': 12,
    //                    'elemePart': 0.0,
    //                    'name': '(不与美食活动同享)在线支付立减优惠',
    //                    'restaurantPart': 0.0
    //                }
    //            ],
    //            'merchantActivityPart': -6.0,
    //            'merchantDeliverySubsidy': 0.0,
    //            'orderLatestStatus': '等待接单',
    //            'orderTraceButton': 'NONE',
    //            'orderTraceRenderViews': [],
    //            'orderType': 'BOOKING',
    //            'packageFee': 3.0,
    //            'payAmount': 42.0,
    //            'payment': 'ONLINE',
    //            'paymentStatus': 'SUCCESS',
    //            'phoneAlertDescription': '',
    //            'remark': '',
    //            'restaurantName': '猿串（解放西路店）',
    //            'restaurantPart': -6.0,
    //            'secretPhoneExpireTime': '2017-03-02T20:45:00',
    //            'serviceFee': -2.35,
    //            'serviceRate': 0.05,
    //            'shopId': 848415,
    //            'shopName': '猿串（解放西路店）',
    //            'showAgreeCancelOrderButton': false,
    //            'showAgreeRefundOrderButton': false,
    //            'showConfirmOrderButton': true,
    //            'showDeliveryBySelfOnCancelled': false,
    //            'showDisagreeCancelOrderButton': false,
    //            'showDisagreeRefundOrderButton': false,
    //            'showInvalidOrderButton': true,
    //            'showNoMoreDeliveryButton': false,
    //            'showPrintOrderButton': false,
    //            'showReadCancelOrderButton': false,
    //            'showReadExceptionOrderButton': false,
    //            'showSetDeliveryBySelfButton': false,
    //            'status': 'UNPROCESSED',
    //            'times': 6,
    //            'tips': [
    //                {
    //                    'backColor': '#ffb651',
    //                    'content': '预订单，请于03-02 17:45送达',
    //                    'contentColor': '#ffffff',
    //                    'priority': 200,
    //                    'tipCategory': 'NOTICE'
    //                }
    //            ],
    //            'userTips': [
    //                {
    //                    'backColor': '#FFFFFF',
    //                    'content': '第6次下单',
    //                    'contentColor': '#999999',
    //                    'priority': -1,
    //                    'tipCategory': 'TIP'
    //                }
    //            ],
    //            'vipDeliveryFeeDiscount': 0.0
    //        }
    //    ]
    //}";
    //        JSONObject jo = JSON.parseObject(joStr);
    //        
    //        Random rand = new Random();
    //        StringBuffer sb = new StringBuffer();
    //        
    //        for (int i = 0; i < 19; i++) {
    //            sb.append(rand.nextInt(10));
    //        }
    //        
    //        JSONArray reJa = jo.getJSONArray("result");
    //        JSONObject reJo = reJa.getJSONObject(0);
    //        reJo.put("id", sb);
    //        
    //        return jo.toString();
    //    }
}
