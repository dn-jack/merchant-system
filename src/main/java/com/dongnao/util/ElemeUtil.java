package com.dongnao.util;

import com.alibaba.fastjson.JSONObject;

public class ElemeUtil {
    
    public static String confirmOrderUrl = "https://app-api.shop.ele.me/order/invoke/?method=order.confirmOrder";
    
    public static JSONObject confirmOrderJo() {
        JSONObject paramJo = new JSONObject();
        paramJo.put("id", "3c9691a3-39cd-4324-843b-7d6d54c270a9");
        paramJo.put("method", "confirmOrder");
        paramJo.put("service", "order");
        JSONObject params = new JSONObject();
        params.put("orderId", "1203255090059113652");
        paramJo.put("params", params);
        JSONObject metas = new JSONObject();
        metas.put("appName", "melody");
        metas.put("appVersion", "4.4.0");
        metas.put("ksid", "NTE4MDkwYjItNDlmNy00ZmIyLWI4ODOWMyOT");
        paramJo.put("metas", metas);
        paramJo.put("ncp", "2.0.0");
        return paramJo;
    }
    
}
