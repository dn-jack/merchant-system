package com.dongnao.controller;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dongnao.util.ElemeUtil;
import com.dongnao.util.HttpRequest;
import com.dongnao.util.HttpUtil;
import com.dongnao.util.HttpsRequestUtil;
import com.dongnao.util.JsonUtil;
import com.dongnao.util.SpringContextHolder;
import com.dongnao.util.UrlUtil;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

@Controller
@RequestMapping("/query")
public class QueryOrderController {
    
    private static transient Logger log = LoggerFactory.getLogger(QueryOrderController.class);
    
    /** 
     * @Fields elmCookiesMap 饿了么cookie缓存
     */
    Map<String, String> elmCookiesMap = new HashMap<String, String>();
    
    /** 
     * @Fields mtCookiesMap 美团cookie缓存
     */
    Map<String, String> mtCookiesMap = new HashMap<String, String>();
    
    @RequestMapping("/elmQueryOrder")
    public @ResponseBody String elmQueryOrder(HttpServletRequest request,
            @RequestBody String param) {
        JSONObject retJo = new JSONObject();
        try {
            JSONObject paramJo = JSON.parseObject(param);
            String username = JsonUtil.getString(paramJo, "username");
            String password = JsonUtil.getString(paramJo, "password");
            String shopId = JsonUtil.getString(paramJo, "shopId");
            JSONArray retresult = new JSONArray();
            
            String ksid = getKsid(username);
            
            //如果缓存中没有，就代表没有登录，需要模拟登录
            if (JsonUtil.isBlank(ksid)) {
                String loginStr = elmLogin(username, password);
                JSONObject loginJo = JSON.parseObject(loginStr);
                if ("9999".equals(loginJo.getString("respCode"))) {
                    return loginStr;
                }
            }
            
            JSONObject pollJo = ElemeUtil.pollForHighJo();
            pollJo.put("shopId", shopId);
            pollJo.getJSONObject("params").put("shopId", shopId);
            JSONObject metas = pollJo.getJSONObject("metas");
            metas.put("ksid", ksid != null ? ksid : getKsid(username));
            
            String result = HttpsRequestUtil.doPost(ElemeUtil.pollForHighUrl,
                    pollJo.toString(),
                    "UTF-8",
                    300000,
                    300000);
            
            log.info("---------------饿了么pollForHighUrl饿了么----------------"
                    + result);
            
            if (result != null && !"".equals(result)) {
                JSONObject resultJo = JSON.parseObject(result);
                JSONObject reJo = resultJo.getJSONObject("result");
                if (reJo == null) {
                    retJo.put("respCode", "9999");
                    retJo.put("respDesc", "系统繁忙！");
                    return retJo.toString();
                }
                JSONArray newOrderIdsJa = reJo.getJSONArray("newOrderIds");
                
                if (JsonUtil.isNotBlank(newOrderIdsJa)) {
                    
                    JSONObject countJo = ElemeUtil.countOrderJo();
                    JSONObject queryJo = ElemeUtil.queryOrderJo();
                    countJo.getJSONObject("metas").put("ksid",
                            ksid != null ? ksid : getKsid(username));
                    countJo.getJSONObject("params").put("shopId", shopId);
                    //                    countJo.getJSONObject("params")
                    //                            .getJSONObject("condition")
                    //                            .put("beginTime", aaa + "T00:00:00");
                    //                    countJo.getJSONObject("params")
                    //                            .getJSONObject("condition")
                    //                            .put("endTime", aaa + "T23:59:59");
                    log.info(countJo.toJSONString());
                    String countRe = HttpsRequestUtil.doPost(ElemeUtil.countOrderurl,
                            countJo.toString(),
                            "UTF-8",
                            300000,
                            300000);
                    log.info(countRe);
                    JSONObject count = JSON.parseObject(countRe);
                    if (count.get("result") != null) {
                        queryJo.getJSONObject("params")
                                .getJSONObject("condition")
                                .put("limit", count.getInteger("result"));
                    }
                    
                    queryJo.getJSONObject("params").put("shopId", shopId);
                    queryJo.getJSONObject("metas").put("ksid",
                            ksid != null ? ksid : getKsid(username));
                    String queryRe = HttpsRequestUtil.doPost(ElemeUtil.queryOrderurl,
                            queryJo.toString(),
                            "UTF-8",
                            300000,
                            300000);
                    //            String queryRe = HttpRequest.sendPost(ElemeUtil.queryOrderurl, "");
                    
                    log.info("---------------饿了么queryOrderurl饿了么----------------"
                            + queryRe);
                    
                    JSONObject queryReJo = JSON.parseObject(queryRe);
                    JSONArray qureJa = queryReJo.getJSONArray("result");
                    
                    if (JsonUtil.isNotBlank(qureJa)) {
                        JSONArray reJa = queryReJo.getJSONArray("result");
                        
                        for (Object o : reJa) {
                            JSONObject eachJo = (JSONObject)o;
                            try {
                                retresult.add(elmFixData(eachJo,
                                        username,
                                        password,
                                        shopId));
                            }
                            catch (Exception e) {
                                continue;
                            }
                        }
                    }
                }
            }
            
            retJo.put("respCode", "0000");
            retJo.put("respDesc", "查询成功！");
            retJo.put("result", retresult);
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
    
    private String elmLogin(String username, String password) {
        try {
            JSONObject loginJo = ElemeUtil.loginJo();
            JSONObject paramsJo = loginJo.getJSONObject("params");
            paramsJo.put("username", username);
            paramsJo.put("password", password);
            JSONObject retJo = new JSONObject();
            
            String loginRe = HttpsRequestUtil.doPost(ElemeUtil.loginurl,
                    loginJo.toString(),
                    "UTF-8",
                    300000,
                    300000);
            
            log.info("---------------饿了么loginurl饿了么----------------" + loginRe);
            String ksid = null;
            JSONObject loginRejo = JSON.parseObject(loginRe);
            if (JsonUtil.isNotBlank(loginRejo.get("result"))
                    && loginRejo.getJSONObject("result").getBoolean("succeed")) {
                ksid = loginRejo.getJSONObject("result")
                        .getJSONObject("successData")
                        .getString("ksid");
                elmCookiesMap.put(username, ksid);
                insertToDn_ksid(username, ksid);
                retJo.put("respCode", "0000");
                retJo.put("respDesc", "登录成功！");
                return retJo.toString();
            }
            retJo.put("respCode", "9999");
            retJo.put("respDesc", "登录失败！");
            return retJo.toString();
        }
        catch (Exception e) {
            dn_errorOrder(username,
                    password,
                    "",
                    "elm",
                    null,
                    null,
                    e.getMessage());
            e.printStackTrace();
            JSONObject retJo = new JSONObject();
            retJo.put("respCode", "9999");
            retJo.put("respDesc", e.getMessage());
            return retJo.toString();
        }
    }
    
    private void insertMongodbByUserName(String userName, String cookies) {
        MongoTemplate mt = (MongoTemplate)SpringContextHolder.getWebApplicationContext()
                .getBean("mongoTemplate");
        BasicDBObject userNameBo = new BasicDBObject();
        BasicDBObject cookieBo = new BasicDBObject();
        userNameBo.put("username", userName);
        cookieBo.put("username", userName);
        cookieBo.put("cookies", cookies);
        DBCollection dbc = mt.getCollection("dn_cookies");
        dbc.remove(userNameBo);
        mt.remove(userNameBo, "dn_cookies");
        mt.insert(cookieBo, "dn_cookies");
    }
    
    private void insertToDn_ksid(String username, String ksid) throws Exception {
        MongoTemplate mt = (MongoTemplate)SpringContextHolder.getWebApplicationContext()
                .getBean("mongoTemplate");
        
        JSONObject relJo = new JSONObject();
        relJo.put("username", username);
        relJo.put("ksid", ksid);
        
        BasicDBObject obj = new BasicDBObject();
        obj.put("username", username);
        DBCollection dbc = mt.getCollection("dn_ksid");
        dbc.remove(obj);
        mt.remove(obj, "dn_ksid");
        mt.insert(relJo.toString(), "dn_ksid");
    }
    
    private String getKsid(String username) throws Exception {
        MongoTemplate mt = (MongoTemplate)SpringContextHolder.getWebApplicationContext()
                .getBean("mongoTemplate");
        
        DBCollection dbc = mt.getCollection("dn_ksid");
        BasicDBObject cond1 = new BasicDBObject();
        cond1.put("username", username);
        DBCursor cursor = dbc.find(cond1);
        
        String ksid = null;
        
        while (cursor.hasNext()) {
            DBObject dbo = cursor.next();
            ksid = dbo.get("ksid").toString();
        }
        return ksid;
    }
    
    private String getMtCookies(String username) throws Exception {
        MongoTemplate mt = (MongoTemplate)SpringContextHolder.getWebApplicationContext()
                .getBean("mongoTemplate");
        
        DBCollection dbc = mt.getCollection("dn_cookies");
        BasicDBObject cond1 = new BasicDBObject();
        cond1.put("username", username);
        DBCursor cursor = dbc.find(cond1);
        
        String cookies = null;
        
        while (cursor.hasNext()) {
            DBObject dbo = cursor.next();
            cookies = dbo.get("cookies").toString();
        }
        return cookies;
    }
    
    private JSONObject elmFixData(JSONObject jo, String username,
            String password, String shopId) throws Exception {
        JSONObject fixJo = new JSONObject();
        try {
            fixJo.put("orderTime", JsonUtil.getString(jo, "activeTime")
                    .replace("T", " "));
            fixJo.put("orderNo", JsonUtil.getString(jo, "id"));
            fixJo.put("userName", JsonUtil.getString(jo, "consigneeName"));
            //        fixJo.put("sex", JsonUtil.getString(fixJo, "consigneeName"));
            fixJo.put("phone", jo.getJSONArray("consigneePhones").get(0));
            fixJo.put("merchantId", JsonUtil.getString(jo, "shopId"));
            fixJo.put("platformCount", JsonUtil.getString(jo, "daySn"));
            String status = JsonUtil.getString(jo, "status");
            
            if ("INVALID".equals(status)) {
                fixJo.put("is_invalid", "1");
            }
            else {
                fixJo.put("is_invalid", "0");
            }
            
            JSONArray groupsJa = jo.getJSONArray("groups");
            JSONArray dishesJa = new JSONArray();
            for (Object o : groupsJa) {
                JSONObject groupsJo = (JSONObject)o;
                if ("NORMAL".equals(groupsJo.getString("type"))) {
                    JSONArray itemsJa = groupsJo.getJSONArray("items");
                    
                    for (Object itemO : itemsJa) {
                        JSONObject itemJo = (JSONObject)itemO;
                        JSONObject dishesJo = new JSONObject();
                        dishesJo.put("dishName",
                                JsonUtil.getString(itemJo, "name"));
                        dishesJo.put("activityName", "特价");
                        dishesJo.put("count",
                                JsonUtil.getString(itemJo, "quantity"));
                        dishesJo.put("price1",
                                JsonUtil.getString(itemJo, "price"));
                        dishesJo.put("price2",
                                JsonUtil.getString(itemJo, "total"));
                        dishesJo.put("goods_id",
                                JsonUtil.getString(itemJo, "id"));
                        dishesJo.put("goods_price",
                                JsonUtil.getString(itemJo, "price"));
                        dishesJa.add(dishesJo);
                    }
                }
                else if ("EXTRA".equals(groupsJo.getString("type"))) {
                    JSONArray itemsJa = groupsJo.getJSONArray("items");
                    
                    for (Object itemO : itemsJa) {
                        JSONObject itemJo = (JSONObject)itemO;
                        if ("102".equals(itemJo.getString("categoryId"))
                                || "餐盒".equals(itemJo.getString("name"))) {
                            fixJo.put("boxPrice",
                                    JsonUtil.getString(itemJo, "price"));
                        }
                    }
                }
            }
            
            fixJo.put("dishes", dishesJa);
            fixJo.put("distributionPrice", "");
            fixJo.put("discount", "");
            fixJo.put("hongbao", JsonUtil.getString(jo, "hongbao"));
            fixJo.put("orderPrice",
                    JsonUtil.getString(jo, "goodsTotalWithoutPackage"));
            fixJo.put("state", "0");
            fixJo.put("merchantName", JsonUtil.getString(jo, "shopName"));
            String orderType = JsonUtil.getString(jo, "orderType");
            if ("BOOKING_UNPROCESSED".equals(orderType)) {
                orderType = "BOOKING";
            }
            fixJo.put("orderType", orderType);
            fixJo.put("merchantActivityPart",
                    JsonUtil.getString(jo, "merchantActivityPart"));
            fixJo.put("elemeActivityPart",
                    JsonUtil.getString(jo, "elemeActivityPart"));
            fixJo.put("serviceFee", JsonUtil.getString(jo, "serviceFee"));
            fixJo.put("serviceRate", JsonUtil.getString(jo, "serviceRate"));
            
            //        fixJo.put("platform_dist_charge",
            //                JsonUtil.getString(jo, "deliveryFeeTotal"));
            fixJo.put("settlement_amount", JsonUtil.getString(jo, "income"));
            fixJo.put("distribution_mode",
                    JsonUtil.getString(jo, "deliveryServiceType"));
            fixJo.put("remark", JsonUtil.getString(jo, "remark"));
            fixJo.put("platform_type", "elm");
            fixJo.put("booked_time", JsonUtil.getString(jo, "bookedTime"));
            fixJo.put("consignee_name", JsonUtil.getString(jo, "consigneeName"));
            fixJo.put("active_time", JsonUtil.getString(jo, "activeTime"));
            fixJo.put("active_total", JsonUtil.getString(jo, "activityTotal"));
            fixJo.put("orderLatestStatus",
                    JsonUtil.getString(jo, "orderLatestStatus"));
            fixJo.put("consigneeAddress",
                    JsonUtil.getString(jo, "consigneeAddress"));
            fixJo.put("distance", JsonUtil.getString(jo, "distance"));
            
            //20170508新增
            double amount = 0.0;
            if (JsonUtil.isNotBlank(jo.get("merchantActivities"))) {
                JSONArray merchantActivities = jo.getJSONArray("merchantActivities");
                for (Object o : merchantActivities) {
                    JSONObject merchantActivitieJo = (JSONObject)o;
                    if ("商家代金券抵扣".equals(merchantActivitieJo.getString("name")
                            .trim())) {
                        amount += merchantActivitieJo.getDouble("amount");
                    }
                    //                else if ("红包抵扣".equals(merchantActivitieJo.getString("name")
                    //                        .trim())) {
                    //                    amount += merchantActivitieJo.getDouble("amount");
                    //                }
                }
                fixJo.put("merchant_subsidy_vouchers", amount);
            }
            fixJo.put("merchant_activities_subsidies",
                    jo.getDouble("restaurantPart") - amount);
            String deliveryServiceType = JsonUtil.getString(jo,
                    "deliveryServiceType");
            if ("CROWD".equals(deliveryServiceType)) {
                fixJo.put("merchant_dist_charge",
                        JsonUtil.getString(jo, "deliveryFeeTotal"));
            }
            else {
                fixJo.put("merchant_dist_charge", "0");
            }
            
            if ("CONTROLLED".equals(deliveryServiceType)) {
                fixJo.put("platform_dist_charge",
                        JsonUtil.getString(jo, "deliveryFeeTotal"));
                fixJo.put("order_dist_charge ",
                        JsonUtil.getString(jo, "deliveryFeeTotal"));
            }
            else {
                fixJo.put("platform_dist_charge", "0");
                fixJo.put("order_dist_charge ", "0");
            }
            fixJo.put("distribution_mode",
                    JsonUtil.getString(jo, "deliveryServiceType"));
            fixJo.put("platform_activities_subsidies",
                    JsonUtil.getString(jo, "elemeActivityPart"));
        }
        catch (Exception e) {
            e.printStackTrace();
            log.info(e.getMessage());
            dn_errorOrder(username,
                    password,
                    shopId,
                    "elm",
                    jo,
                    null,
                    e.getMessage());
            throw e;
        }
        //20170508新增
        return fixJo;
    }
    
    private void dn_errorOrder(String username, String password, String shopId,
            String platform, JSONObject jo, JSONObject chargeJo, String msg) {
        MongoTemplate mt = (MongoTemplate)SpringContextHolder.getWebApplicationContext()
                .getBean("mongoTemplate");
        
        BasicDBObject relJo = new BasicDBObject();
        relJo.put("username", username);
        relJo.put("password", password);
        relJo.put("shopId", shopId);
        relJo.put("platformType", platform);
        relJo.put("json", jo);
        relJo.put("charge", chargeJo);
        relJo.put("msg", msg);
        relJo.put("channel", "queryOrder");
        
        BasicDBObject shopIds = new BasicDBObject();
        shopIds.put("username", username);
        shopIds.put("password", password);
        DBCollection dbc = mt.getCollection("dn_errorOrder");
        dbc.remove(shopIds);
        mt.remove(shopIds, "dn_errorOrder");
        mt.insert(relJo, "dn_errorOrder");
    }
    
    @RequestMapping("/mtQueryOrder")
    public @ResponseBody String mtQueryOrder(HttpServletRequest request,
            @RequestBody String param) {
        
        JSONObject retJo = new JSONObject();
        try {
            JSONObject paramJo = JSON.parseObject(param);
            String username = JsonUtil.getString(paramJo, "username");
            String password = JsonUtil.getString(paramJo, "password");
            JSONArray retresult = new JSONArray();
            
            String cookies = getMtCookies(username);
            
            if (JsonUtil.isBlank(cookies)) {
                log.info("美团step1--------------------->invoke美团");
                JSONObject step1Ret = step1(username, password);
                
                if (step1Ret.containsKey("Location")) {
                    log.info("美团step2--------------------->invoke美团");
                    Map<String, String> step2Ret = step2(step1Ret.getString("Location"));
                    if (step2Ret.containsKey("BSID")
                            && step2Ret.containsKey("entryList")
                            && step2Ret.containsKey("device_uuid")) {
                        log.info("美团step3--------------------->invoke美团");
                        step3(step2Ret, username);
                    }
                }
            }
            
            if (JsonUtil.isBlank(getMtCookies(username))) {
                dn_errorOrder(username,
                        password,
                        "",
                        "mt",
                        null,
                        null,
                        "用户名密码错误，登录失败！");
                retJo.put("respCode", "9999");
                retJo.put("respDesc", "登录失败！");
                return retJo.toString();
            }
            
            String getStr = "?time=" + System.currentTimeMillis()
                    + "&isQuery=0&getNewVo=1";
            log.info("美团ofq--------------------->queryString:美团" + getStr);
            
            String getret = HttpRequest.sendGet(UrlUtil.MT_QO + getStr,
                    cookies != null ? cookies : getMtCookies(username));
            log.info("美团ofq--------------------->result:美团" + getret);
            
            JSONObject orderJo = JSON.parseObject(getret);
            if (JsonUtil.isNotBlank(orderJo.get("data"))) {
                JSONArray dataJa = orderJo.getJSONArray("data");
                for (Object o : dataJa) {
                    JSONObject eachData = (JSONObject)o;
                    
                    log.info("美团chargeInfo--------------------->queryString:美团"
                            + getStr);
                    
                    Map<String, String> chargeInfo = HttpRequest.sendPost1(UrlUtil.MT_CI,
                            "chargeInfo=[{wmOrderViewId:"
                                    + JsonUtil.getString(eachData,
                                            "wm_order_id_view") + ",wmPoiId:"
                                    + JsonUtil.getString(eachData, "wm_poi_id")
                                    + "}]",
                            cookies != null ? cookies : getMtCookies(username));
                    log.info("美团chargeInfo--------------------->result:美团"
                            + chargeInfo.get("result"));
                    
                    String chargeRet = chargeInfo.get("result");
                    JSONObject chargeJo = JSON.parseObject(chargeRet);
                    if (JsonUtil.isNotBlank(chargeJo.get("data"))) {
                        try {
                            retresult.add(mtfixData(eachData,
                                    chargeJo.getJSONArray("data")
                                            .getJSONObject(0),
                                    username,
                                    password));
                        }
                        catch (Exception e) {
                            continue;
                        }
                    }
                }
            }
            
            retJo.put("respCode", "0000");
            retJo.put("respDesc", "查询成功！");
            retJo.put("result", retresult);
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
    
    private JSONObject step1(String userName, String password) {
        StringBuffer sb = new StringBuffer();
        sb.append("login=" + userName)
                .append("&password=" + password)
                .append("&part_key=")
                .append("&captcha_code=")
                .append("&captcha_v_token=")
                .append("&sms_verify=" + 0)
                .append("&sms_code=");
        log.info("美团step1--------------------->param:美团" + sb.toString());
        JSONObject ret = null;
        try {
            ret = HttpsRequestUtil.doPost1(UrlUtil.MT_loginurl,
                    sb.toString(),
                    "UTF-8",
                    300000,
                    300000);
            log.info("美团step1--------------------->result:美团" + ret.toString());
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return ret;
    }
    
    private Map<String, String> step2(String location) {
        
        Map<String, String> retMap = new HashMap<String, String>();
        String BSID = location.split("\\?")[1].replace("BSID=", "");
        retMap.put("BSID", BSID);
        log.info("美团step2--------------------->BSID:美团" + BSID);
        
        List<String> entryList = HttpRequest.sendGet1(location);
        
        StringBuffer sb1 = new StringBuffer();
        for (String entry : entryList) {
            sb1.append(entry).append(";");
        }
        retMap.put("entryList", sb1.toString());
        log.info("美团step2--------------------->entryList:美团"
                + entryList.toString());
        
        String device_uuid = "";
        
        for (String ele : entryList) {
            if (ele.contains("device_uuid")) {
                String replaceStr = ele.replace("device_uuid=", "");
                device_uuid = replaceStr.substring(0, replaceStr.indexOf(";"));
            }
        }
        retMap.put("device_uuid", device_uuid);
        log.info("美团step2--------------------->device_uuid:美团" + device_uuid);
        return retMap;
    }
    
    private Map<String, String> step3(Map<String, String> step2, String userName) {
        Map<String, String> retMap = HttpRequest.sendPost1(UrlUtil.MT_logon,
                "BSID=" + step2.get("BSID") + "&device_uuid="
                        + step2.get("device_uuid") + "&service=",
                step2.get("entryList"));
        log.info("美团step3--------------------->retMap:美团" + retMap);
        mtCookiesMap.put(userName,
                step2.get("entryList") + retMap.get("cookie"));
        insertMongodbByUserName(userName,
                step2.get("entryList") + retMap.get("cookie"));
        return retMap;
    }
    
    private String dateParse(long mesc) {
        SimpleDateFormat dateformat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss");
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(mesc);
        return dateformat.format(gc.getTime());
    }
    
    private JSONObject mtfixData(JSONObject jo, JSONObject chargeJo,
            String userName, String password) {
        JSONObject fixJo = new JSONObject();
        try {
            log.info("------------------------orderJo--------------------" + jo);
            
            fixJo.put("orderTime", JsonUtil.getString(jo, "order_time_fmt"));
            fixJo.put("orderNo", JsonUtil.getString(jo, "wm_order_id_view_str"));
            fixJo.put("userName", JsonUtil.getString(jo, "recipient_name"));
            fixJo.put("phone", JsonUtil.getString(jo, "recipient_phone"));
            fixJo.put("merchantId", userName);
            fixJo.put("platformCount", JsonUtil.getString(jo, "num"));
            
            String status = JsonUtil.getString(jo, "status");
            if ("9".equals(status)) {
                fixJo.put("is_invalid", "1");
            }
            else {
                fixJo.put("is_invalid", "0");
            }
            
            double prices = 0.0;
            
            if (JsonUtil.isNotBlank(jo.get("cartDetailVos"))) {
                JSONArray dishesJa = new JSONArray();
                for (Object o : jo.getJSONArray("cartDetailVos")) {
                    JSONObject cartDetailVosJo = (JSONObject)o;
                    if (JsonUtil.isNotBlank(cartDetailVosJo.get("details"))) {
                        JSONArray detailsJa = cartDetailVosJo.getJSONArray("details");
                        for (Object detailso : detailsJa) {
                            JSONObject detailJo = (JSONObject)detailso;
                            JSONObject dishJo = new JSONObject();
                            dishJo.put("dishName",
                                    JsonUtil.getString(detailJo, "food_name"));
                            //                        dishJo.put("activityName", "特价");
                            dishJo.put("count",
                                    JsonUtil.getString(detailJo, "count"));
                            dishJo.put("price1", JsonUtil.getString(detailJo,
                                    "origin_food_price"));
                            dishJo.put("price2",
                                    JsonUtil.getString(detailJo, "food_price"));
                            dishJo.put("goods_id",
                                    JsonUtil.getString(detailJo, "wm_food_id"));
                            dishJo.put("goods_price",
                                    JsonUtil.getString(detailJo,
                                            "origin_food_price"));
                            if (JsonUtil.isNotBlank(detailJo.get("origin_food_price"))) {
                                prices += detailJo.getDouble("origin_food_price");
                            }
                            dishesJa.add(dishJo);
                        }
                    }
                }
                fixJo.put("dishes", dishesJa);
            }
            
            if (chargeJo.containsKey("giftDetails")
                    && JsonUtil.isNotBlank(chargeJo.get("giftDetails"))) {
                JSONArray gifts = chargeJo.getJSONArray("giftDetails");
                for (Object gift : gifts) {
                    JSONObject giftJo = (JSONObject)gift;
                    if (JsonUtil.isNotBlank(giftJo.get("giftAmount"))) {
                        prices += giftJo.getDouble("giftAmount");
                    }
                }
            }
            
            fixJo.put("boxPrice", JsonUtil.getString(jo, "boxPriceTotal"));
            fixJo.put("orderPrice", prices);
            fixJo.put("state", "0");
            fixJo.put("merchantName", JsonUtil.getString(jo, "poi_name"));
            fixJo.put("platform_type", "mt");
            fixJo.put("consignee_name",
                    JsonUtil.getString(jo, "recipient_name"));
            fixJo.put("consigneeAddress",
                    JsonUtil.getString(jo, "recipient_address"));
            fixJo.put("distance", JsonUtil.getString(jo, "distance"));
            fixJo.put("remark", JsonUtil.getString(jo, "remark"));
            
            //        boolean riderPay = JsonUtil.getBoolean(chargeJo, "riderPay");
            //        if (riderPay) {
            //            fixJo.put("platform_dist_charge",
            //                    JsonUtil.getString(chargeJo, "shippingAmount"));
            //            fixJo.put("distribution_mode", "CROWD");
            //        }
            //        else {
            //            fixJo.put("merchant_dist_charge",
            //                    JsonUtil.getString(jo, "shipping_fee"));
            //            fixJo.put("distribution_mode", "NONE");
            //        }
            //        if (JsonUtil.isNotBlank(chargeJo.get("commisionDetails"))) {
            //            fixJo.put("serviceFee",
            //                    JsonUtil.getString(chargeJo.getJSONArray("commisionDetails")
            //                            .getJSONObject(0),
            //                            "chargeAmount"));
            //        }
            //        String status = JsonUtil.getString(jo, "status");
            if ("2".equals(status)) {
                fixJo.put("orderLatestStatus", "等待接单");
            }
            else if ("4".equals(status)) {
                fixJo.put("orderLatestStatus", "等待配送");
            }
            else if ("8".equals(status)) {
                fixJo.put("orderLatestStatus", "用户已确认收餐");
            }
            else if ("9".equals(status)) {
                fixJo.put("orderLatestStatus", "订单取消");
            }
            else {
                fixJo.put("orderLatestStatus", "等待配送");
            }
            fixJo.put("activities_subsidy_bymerchant",
                    jo.getDouble("total_before") - jo.getDouble("total_after"));
            
            //20170508新增
            fixJo.put("orderType", JsonUtil.getString(jo, "orderCopyContent")
                    .contains("预订单") ? "BOOKING" : "NORMAL");
            fixJo.put("merchant_activities_subsidies",
                    JsonUtil.getString(chargeJo, "activityAmount"));
            String shippingType = JsonUtil.getString(chargeJo, "shippingType");
            if ("0000".equals(shippingType)) {
                fixJo.put("merchant_dist_charge",
                        JsonUtil.getString(chargeJo, "shippingAmount"));
            }
            else {
                fixJo.put("merchant_dist_charge", "0");
            }
            
            if ("1001".equals(shippingType)) {
                fixJo.put("platform_dist_charge",
                        JsonUtil.getString(chargeJo, "shippingAmount"));
                fixJo.put("order_dist_charge ",
                        JsonUtil.getString(chargeJo, "shippingAmount"));
            }
            else {
                fixJo.put("platform_dist_charge", "0");
                fixJo.put("order_dist_charge ", "0");
            }
            fixJo.put("serviceFee",
                    JsonUtil.getString(chargeJo, "commisionAmount"));
            fixJo.put("settlement_amount",
                    JsonUtil.getString(chargeJo, "settleAmount"));
            fixJo.put("distribution_mode",
                    JsonUtil.getString(chargeJo, "shippingType"));
            if (JsonUtil.isNotBlank(jo.get("discounts"))) {
                double infocount = 0.0;
                JSONArray discounts = jo.getJSONArray("discounts");
                for (Object o : discounts) {
                    JSONObject discountJo = (JSONObject)o;
                    String info = JsonUtil.getString(discountJo, "info");
                    if (JsonUtil.isNotBlank(info)) {
                        infocount += Double.valueOf(info.substring(info.indexOf("￥") + 1));
                    }
                }
                fixJo.put("platform_activities_subsidies",
                        infocount - chargeJo.getDouble("activityAmount"));
            }
            else {
                fixJo.put("platform_activities_subsidies", 0);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            log.info(e.getMessage());
            dn_errorOrder(userName,
                    password,
                    "",
                    "mt",
                    jo,
                    chargeJo,
                    e.getMessage());
            throw e;
        }
        //20170508新增
        return fixJo;
        
    }
    
    @RequestMapping("/bdwmQueryOrder")
    public @ResponseBody String bdwmQueryOrder(HttpServletRequest request,
            @RequestBody String param) {
        
        JSONObject retJo = new JSONObject();
        try {
            JSONObject paramJo = JSON.parseObject(param);
            String username = JsonUtil.getString(paramJo, "username");
            String password = JsonUtil.getString(paramJo, "password");
            JSONArray retresult = new JSONArray();
            String cookies = queryCookieFromMongo(username);
            
            if (JsonUtil.isBlank(cookies)) {
                retJo.put("respCode", "9999");
                retJo.put("respDesc", "cookies为空，百度外卖模拟登录失败！");
                return retJo.toString();
            }
            
            Map<String, String> params = new HashMap<String, String>();
            Map<String, String> headers = new HashMap<String, String>();
            params.put("qt", "neworderlist");
            params.put("display", "json");
            headers.put("Cookie", cookies);
            log.info("百度外卖-----------------baidu-cookies----------------->百度外卖"
                    + cookies);
            String orderInfo = HttpUtil.doGet(UrlUtil.BD_neworderlist,
                    cookies,
                    "");
            log.info("百度外卖------------------orderinfo---------------------->百度外卖"
                    + orderInfo);
            JSONObject orderJo = JSON.parseObject(orderInfo);
            if (0 != orderJo.getInteger("errno")) {
                retJo.put("respCode", "9999");
                retJo.put("respDesc", "获取订单失败！");
                return retJo.toString();
            }
            JSONObject dataJo = orderJo.getJSONObject("data");
            JSONArray order_list = dataJo.getJSONArray("order_list");
            if (JsonUtil.isBlank(order_list)) {
                retJo.put("respCode", "0000");
                retJo.put("respDesc", "查询成功！");
                retJo.put("result", retresult);
                return retJo.toString();
            }
            for (Object o : order_list) {
                JSONObject order = (JSONObject)o;
                try {
                    retresult.add(bdwmfixData(order, username, password));
                }
                catch (Exception e) {
                    continue;
                }
            }
            retJo.put("respCode", "0000");
            retJo.put("respDesc", "查询成功！");
            retJo.put("result", retresult);
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
    
    private String queryCookieFromMongo(String username) {
        MongoTemplate mt = (MongoTemplate)SpringContextHolder.getWebApplicationContext()
                .getBean("mongoTemplate");
        
        DBCollection dbc = mt.getCollection("dn_bdloginInfo");
        BasicDBObject cond1 = new BasicDBObject();
        cond1.put("username", username);
        DBCursor cursor = dbc.find(cond1);
        
        String cookies = null;
        
        while (cursor.hasNext()) {
            DBObject dbo = cursor.next();
            cookies = dbo.get("loginInfo").toString();
        }
        return cookies;
    }
    
    private JSONObject bdwmfixData(JSONObject jo, String userName,
            String password) throws Exception {
        JSONObject fixJo = new JSONObject();
        try {
            fixJo.put("merchantId", userName);
            fixOrder_basic(fixJo, jo);
            fixOrder_goods(fixJo, jo);
            fixorder_meal_fee(fixJo, jo);
            //        fixorder_total(fixJo, jo);
            fixJo.put("state", "0");
            
            String status = JsonUtil.getString(jo.getJSONObject("order_basic"),
                    "status");
            if ("10".equals(status)) {
                fixJo.put("is_invalid", "1");
            }
            else {
                fixJo.put("is_invalid", "0");
            }
            
            fixJo.put("platform_type", "bdwm");
            //        fixJo.put("merchantActivityPart", getmerchantPart(jo));
            fixJo.put("serviceFee", getserviceFee(jo));
            merchant_activities_subsidies(fixJo, jo);
            merchant_dist_charge(fixJo, jo);
            //        platform_dist_charge(fixJo, jo);
            settlement_amount(fixJo, jo);
            distribution_mode(fixJo, jo);
        }
        catch (Exception e) {
            e.printStackTrace();
            log.info(e.getMessage());
            dn_errorOrder(userName,
                    password,
                    "",
                    "bdwm",
                    jo,
                    null,
                    e.getMessage());
            throw e;
        }
        return fixJo;
    }
    
    private void distribution_mode(JSONObject fixJo, JSONObject jo)
            throws Exception {
        String is_baidu_logistics = JsonUtil.getString(jo.getJSONObject("order_basic"),
                "is_baidu_logistics");
        fixJo.put("distribution_mode",
                "1".equals(is_baidu_logistics) ? "CONTROLLED" : "CROWD");
    }
    
    private void settlement_amount(JSONObject fixJo, JSONObject jo)
            throws Exception {
        if (JsonUtil.isBlank(jo.get("order_total"))) {
            return;
        }
        
        JSONObject order_total = jo.getJSONObject("order_total");
        fixJo.put("settlement_amount",
                JsonUtil.getString(order_total, "shop_price"));
    }
    
    private void merchant_dist_charge(JSONObject fixJo, JSONObject jo)
            throws Exception {
        if (JsonUtil.isBlank(jo.get("takeout_cost"))) {
            return;
        }
        JSONObject takeout_cost = jo.getJSONObject("takeout_cost");
        if (JsonUtil.isBlank(takeout_cost.get("price"))) {
            fixJo.put("merchant_dist_charge", "0");
        }
        String is_baidu_logistics = JsonUtil.getString(jo.getJSONObject("order_basic"),
                "is_baidu_logistics");
        if ("0".equals(is_baidu_logistics)) {
            fixJo.put("merchant_dist_charge",
                    JsonUtil.getBigDecimal(takeout_cost, "price"));
        }
        else {
            fixJo.put("merchant_dist_charge", "0");
        }
    }
    
    private void merchant_activities_subsidies(JSONObject fixJo, JSONObject jo)
            throws Exception {
        if (JsonUtil.isBlank(jo.get("order_goods"))) {
            return;
        }
        JSONObject order_goods = jo.getJSONObject("order_goods");
        if (JsonUtil.isBlank(order_goods.get("goods_list"))) {
            return;
        }
        JSONArray goods_list = order_goods.getJSONArray("goods_list");
        double shop_total_discount = 0.0;
        for (Object o : goods_list) {
            JSONObject goodJo = (JSONObject)o;
            if (JsonUtil.isBlank(goodJo.get("ext"))) {
                return;
            }
            JSONObject extJo = goodJo.getJSONObject("ext");
            if (JsonUtil.isBlank(extJo.get("shop_total_discount"))) {
                continue;
            }
            String shop_total_discountStr = extJo.getString("shop_total_discount");
            shop_total_discount += Double.parseDouble(shop_total_discountStr.replace("已优惠",
                    ""));
        }
        
        if (JsonUtil.isNotBlank(jo.get("shop_other_discount"))) {
            JSONObject shop_other_discount = jo.getJSONObject("shop_other_discount");
            if (JsonUtil.isNotBlank(shop_other_discount.get("price"))) {
                shop_total_discount += Math.abs(shop_other_discount.getDouble("price"));
            }
        }
        fixJo.put("merchant_activities_subsidies", shop_total_discount);
    }
    
    private Double getserviceFee(JSONObject jo) throws Exception {
        JSONObject extract_commission = jo.getJSONObject("extract_commission");
        if (JsonUtil.isBlank(extract_commission)) {
            return 0.00;
        }
        if (JsonUtil.isBlank(extract_commission.get("commission_total"))) {
            return 0.00;
        }
        return extract_commission.getDouble("commission_total");
    }
    
    private Double getmerchantPart(JSONObject jo) throws Exception {
        JSONObject order_goods = jo.getJSONObject("order_goods");
        if (JsonUtil.isBlank(order_goods)) {
            return 0.00;
        }
        JSONArray goods_list = order_goods.getJSONArray("goods_list");
        if (JsonUtil.isBlank(goods_list)) {
            return 0.00;
        }
        Double discount = 0.00;
        for (Object o : goods_list) {
            JSONObject good = (JSONObject)o;
            JSONObject extJo = good.getJSONObject("ext");
            Double eachDiscount = 0.00;
            if (extJo.containsKey("shop_total_discount")) {
                if (JsonUtil.isBlank(extJo.get("shop_total_discount"))) {
                    eachDiscount = 0.00;
                }
                else {
                    eachDiscount = extJo.getDouble("shop_total_discount");
                }
            }
            else {
                eachDiscount = 0.00;
            }
            discount += eachDiscount;
        }
        JSONObject shop_other_discount = jo.getJSONObject("shop_other_discount");
        Double price = 0.00;
        if (JsonUtil.isBlank(shop_other_discount.get("price"))) {
            price = 0.00;
        }
        else {
            price = shop_other_discount.getDouble("price");
        }
        discount += price;
        
        return discount;
    }
    
    private void fixorder_total(JSONObject fixJo, JSONObject order)
            throws Exception {
        if (!order.containsKey("order_total")) {
            return;
        }
        if (JsonUtil.isBlank(order.get("order_total"))) {
            return;
        }
        fixJo.put("orderPrice",
                JsonUtil.getString(order.getJSONObject("order_total"),
                        "customer_price"));
    }
    
    private void fixorder_meal_fee(JSONObject fixJo, JSONObject order)
            throws Exception {
        if (!order.containsKey("order_meal_fee")) {
            return;
        }
        if (JsonUtil.isBlank(order.get("order_meal_fee"))) {
            return;
        }
        fixJo.put("boxPrice",
                JsonUtil.getString(order.getJSONObject("order_meal_fee"),
                        "price"));
    }
    
    private void fixOrder_goods(JSONObject fixJo, JSONObject order)
            throws Exception {
        JSONObject order_goods = order.getJSONObject("order_goods");
        if (JsonUtil.isBlank(order_goods)) {
            return;
        }
        JSONArray goods_list = order_goods.getJSONArray("goods_list");
        if (JsonUtil.isBlank(goods_list)) {
            return;
        }
        JSONArray dishesJa = new JSONArray();
        double origCount = 0.0;
        for (Object o : goods_list) {
            JSONObject good = (JSONObject)o;
            JSONObject dishJo = new JSONObject();
            dishJo.put("dishName", JsonUtil.getString(good, "name"));
            //                        dishJo.put("activityName", "特价");
            dishJo.put("count", JsonUtil.getString(good, "number"));
            dishJo.put("price1", JsonUtil.getString(good, "orig_price"));
            dishJo.put("price2", JsonUtil.getString(good, "orig_unit_price"));
            dishJo.put("goods_id", "");
            dishJo.put("goods_price", JsonUtil.getString(good, "orig_price"));
            origCount += good.getDouble("orig_price");
            dishesJa.add(dishJo);
        }
        fixJo.put("orderPrice", origCount);
        fixJo.put("dishes", dishesJa);
    }
    
    private void fixOrder_basic(JSONObject fixJo, JSONObject order)
            throws Exception {
        JSONObject order_basic = order.getJSONObject("order_basic");
        if (JsonUtil.isBlank(order_basic)) {
            return;
        }
        
        fixJo.put("platformCount",
                JsonUtil.getString(order_basic, "order_index"));
        fixJo.put("orderTime",
                dateParse(Long.parseLong(JsonUtil.getLong(order_basic,
                        "create_time") + "000")));
        fixJo.put("orderNo", JsonUtil.getString(order_basic, "order_id"));
        fixJo.put("userName", JsonUtil.getString(order_basic, "user_real_name"));
        fixJo.put("phone", JsonUtil.getString(order_basic, "user_phone"));
        fixJo.put("merchantName", JsonUtil.getString(order_basic, "shop_name"));
        fixJo.put("consignee_name",
                JsonUtil.getString(order_basic, "user_real_name"));
        fixJo.put("consigneeAddress",
                JsonUtil.getString(order_basic, "user_address"));
        fixJo.put("distance",
                JsonUtil.getString(order_basic, "shop_user_distance"));
        fixJo.put("orderType", JsonUtil.getString(order_basic, "send_time")
                .indexOf("立即送餐") > -1 ? "NORMAL" : "BOOKING");
    }
    
    public static void main(String[] args) throws Exception {
        //        JSONObject param1 = new JSONObject();
        //        param1.put("username", "15243688033_eleme");
        //        param1.put("password", "dbb888888");
        //        param1.put("shopId", "2357016");
        //        JSONObject param2 = new JSONObject();
        //        param2.put("username", "wmglwk34660");
        //        param2.put("password", "60388065");
        //        JSONObject param3 = new JSONObject();
        //        param3.put("username", "cs15243688033");
        //        param3.put("password", "Ax010392");
        //        System.out.print(new QueryOrderController().elmQueryOrder(null,
        //                param1.toString()));
        //        System.out.print(new QueryOrderController().mtQueryOrder(null,
        //                param2.toString()));
        //        System.out.print(new QueryOrderController().bdwmQueryOrder(null,
        //                param3.toString()));
        
        //        JSONObject countJo = ElemeUtil.countOrderJo();
        //        JSONObject queryJo = ElemeUtil.queryOrderJo();
        //        countJo.getJSONObject("metas").put("ksid",
        //                "YjgzYmRmNmYtZjNjZS00MjVmLWJlNmYzE2Yj");
        //        countJo.getJSONObject("params").put("shopId", 2357016);
        //        //                    countJo.getJSONObject("params")
        //        //                            .getJSONObject("condition")
        //        //                            .put("beginTime", aaa + "T00:00:00");
        //        //                    countJo.getJSONObject("params")
        //        //                            .getJSONObject("condition")
        //        //                            .put("endTime", aaa + "T23:59:59");
        //        log.info(countJo.toJSONString());
        //        String countRe = HttpsRequestUtil.doPost(ElemeUtil.countOrderurl,
        //                countJo.toString(),
        //                "UTF-8",
        //                300000,
        //                300000);
        //        log.info(countRe);
        //        JSONObject count = JSON.parseObject(countRe);
        //        if (count.containsKey("result")) {
        //            queryJo.getJSONObject("params")
        //                    .getJSONObject("condition")
        //                    .put("limit", count.getInteger("result"));
        //        }
        //        
        //        queryJo.getJSONObject("params").put("shopId", 2357016);
        //        queryJo.getJSONObject("metas").put("ksid",
        //                "YjgzYmRmNmYtZjNjZS00MjVmLWJlNmYzE2Yj");
        //        String queryRe = HttpsRequestUtil.doPost(ElemeUtil.queryOrderurl,
        //                queryJo.toString(),
        //                "UTF-8",
        //                300000,
        //                300000);
        //        //            String queryRe = HttpRequest.sendPost(ElemeUtil.queryOrderurl, "");
        //        
        //        log.info("---------------饿了么queryOrderurl饿了么----------------" + queryRe);
        log.info("-￥10.0".substring("-￥10.0".indexOf("￥") + 1));
    }
}
