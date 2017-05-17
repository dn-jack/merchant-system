package com.dongnao.controller;

import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dongnao.service.impl.OrderService;
import com.dongnao.util.ElemeUtil;
import com.dongnao.util.HttpRequest;
import com.dongnao.util.HttpsRequestUtil;
import com.dongnao.util.JsonUtil;
import com.dongnao.util.MD5Encryption;
import com.dongnao.util.Prient;
import com.dongnao.util.SpringContextHolder;
import com.dongnao.util.UrlUtil;
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
    
    Map<String, Map<String, String>> mmm = new HashMap<String, Map<String, String>>();
    
    Map<String, String> islogin = new HashMap<String, String>();
    
    @Autowired
    @Qualifier("mongoTemplate")
    MongoTemplate mt;
    
    @Autowired
    OrderService os;
    
    /** 
     * @Description 验证用户名和密码 
     * @param @param request
     * @param @param response
     * @param @param param
     * @param @return 参数 
     * @return String 返回类型  
     * @throws 
     */
    @RequestMapping("/validateUser")
    public @ResponseBody String validateUser(HttpServletRequest request,
            HttpServletResponse response, @RequestBody String param) {
        
        log.info(param);
        JSONObject paramJo = JSON.parseObject(param);
        String platformType = paramJo.getString("platformType");
        if ("elm".equals(platformType)) {
            return elmUserLogin(paramJo);
        }
        else if ("mt".equals(platformType)) {
            return mtUserLogin(paramJo);
        }
        return null;
    }
    
    @RequestMapping("/orderCount")
    public @ResponseBody String orderCount(HttpServletRequest request,
            HttpServletResponse response, @RequestBody String param) {
        
        JSONObject paramJo = JSON.parseObject(param);
        
        String password = paramJo.getString("password").trim();
        paramJo.remove("password");
        paramJo.put("password", MD5Encryption.MD5(password));
        
        String retStr = HttpRequest.sendPostxx(UrlUtil.countUrl,
                paramJo.toString());
        JSONObject retJo = new JSONObject();
        
        if (JsonUtil.isBlank(retStr)) {
            retJo.put("respCode", "9999");
            retJo.put("respDesc", "请求失败!");
            return retJo.toString();
        }
        return retStr;
    }
    
    private void insertToMongodb(String key, String value) {
        MongoTemplate mt = (MongoTemplate)SpringContextHolder.getWebApplicationContext()
                .getBean("mongoTemplate");
        JSONObject monjo = new JSONObject();
        monjo.put("key", key);
        monjo.put("value", value);
        mt.insert(monjo.toString(), "dn_validateUser");
    }
    
    private boolean validateUserFromMongo(String key) {
        MongoTemplate mt = (MongoTemplate)SpringContextHolder.getWebApplicationContext()
                .getBean("mongoTemplate");
        DBCollection dbc = mt.getCollection("dn_validateUser");
        BasicDBObject cond3 = new BasicDBObject();
        cond3.put("key", key);
        DBCursor cursor = dbc.find(cond3);
        while (cursor.hasNext()) {
            return true;
        }
        return false;
    }
    
    private String elmUserLogin(JSONObject param) {
        JSONObject retJo = new JSONObject();
        try {
            
            if (JsonUtil.isBlank(param.get("username"))
                    || JsonUtil.isBlank(param.get("password"))) {
                retJo.put("respCode", "9999");
                retJo.put("respDesc", "验证失败，用户名或者密码不能为空！");
                return retJo.toString();
            }
            
            String username = param.getString("username");
            String password = param.getString("password");
            
            if (validateUserFromMongo(username + password)) {
                retJo.put("respCode", "0000");
                retJo.put("respDesc", "调用成功且登录验证成功！");
                return retJo.toString();
            }
            
            JSONObject loginJo = ElemeUtil.loginJo();
            JSONObject paramsJo = loginJo.getJSONObject("params");
            paramsJo.put("username", username);
            paramsJo.put("password", password);
            String loginRe = null;
            
            loginRe = HttpsRequestUtil.doPost(ElemeUtil.loginurl,
                    loginJo.toString(),
                    "UTF-8",
                    300000,
                    300000);
            
            log.info("---------------loginurl----------------" + loginRe);
            
            JSONObject loginRejo = JSON.parseObject(loginRe);
            if (JsonUtil.isNotBlank(loginRejo.get("result"))
                    && loginRejo.getJSONObject("result").getBoolean("succeed")) {
                //0000已经登录验证过登录成功 9999已经登录验证过登录失败
                islogin.put("elm" + username, "0000");
                retJo.put("respCode", "0000");
                retJo.put("respDesc", "调用成功且登录验证成功！");
                insertToMongodb(username + password, "0000");
            }
            else {
                islogin.put("elm" + username, "9999");
                retJo.put("respCode", "9999");
                retJo.put("respDesc", "调用成功且登录验证失败！");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            retJo.put("respCode", "9999");
            retJo.put("respDesc", "调用成功且登录验证失败！");
        }
        
        return retJo.toString();
    }
    
    private String mtUserLogin(JSONObject param) {
        JSONObject retJo = new JSONObject();
        try {
            
            if (JsonUtil.isBlank(param.get("username"))
                    || JsonUtil.isBlank(param.get("password"))) {
                retJo.put("respCode", "9999");
                retJo.put("respDesc", "验证失败，用户名或者密码不能为空！");
                return retJo.toString();
            }
            String username = param.getString("username");
            String password = param.getString("password");
            
            if (islogin.containsKey("mt" + username)
                    && "0000".equals(islogin.get("mt" + username))) {
                retJo.put("respCode", "0000");
                retJo.put("respDesc", "调用成功且登录验证成功！");
                return retJo.toString();
            }
            
            log.info("step1--------------------->invoke");
            JSONObject step1Ret = step1(username, password);
            
            if (step1Ret.containsKey("Location")) {
                //                log.info("step2--------------------->invoke");
                //                Map<String, String> step2Ret = step2(step1Ret.getString("Location"));
                //                if (step2Ret.containsKey("BSID")
                //                        && step2Ret.containsKey("entryList")
                //                        && step2Ret.containsKey("device_uuid")) {
                //                    log.info("step3--------------------->invoke");
                //                    step3(step2Ret, username);
                //                }
                islogin.put("mt" + username, "0000");
                retJo.put("respCode", "0000");
                retJo.put("respDesc", "调用成功且登录验证成功！");
            }
            else {
                islogin.put("mt" + username, "9999");
                retJo.put("respCode", "9999");
                retJo.put("respDesc", "调用成功且登录验证失败！");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            retJo.put("respCode", "9999");
            retJo.put("respDesc", "调用成功且登录验证失败！");
        }
        return retJo.toString();
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
        log.info("step1--------------------->param:" + sb.toString());
        JSONObject ret = null;
        try {
            ret = HttpsRequestUtil.doPost1(UrlUtil.MT_loginurl,
                    sb.toString(),
                    "UTF-8",
                    300000,
                    300000);
            log.info("step1--------------------->result:" + ret.toString());
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return ret;
    }
    
    @RequestMapping("/getToten")
    public @ResponseBody String getToten(HttpServletRequest request,
            HttpServletResponse response, @RequestBody String param) {
        
        if (JsonUtil.isNotBlank(param)) {
            JSONObject paramJo = JSON.parseObject(param);
            if (JsonUtil.isNotBlank(paramJo.get("uuid"))) {
                mmm.remove(paramJo.get("uuid"));
            }
        }
        
        Map<String, String> retStrMap = HttpRequest.bdsendGet("https://wmpass.baidu.com/wmpass/openservice/captchapair?protocal=https&callback=jQuery1110015827547668209752_1490844419324&_=1490844419343");
        
        String retStr = retStrMap.get("result");
        String cookies = retStrMap.get("cookie");
        
        Map<String, String> usrToken = new HashMap<String, String>();
        
        usrToken.put("tokenCookie", cookies);
        usrToken.put("tracecode", retStrMap.get("tracecode"));
        usrToken.put("P3P", retStrMap.get("P3P"));
        
        String data = retStr.substring((retStr.indexOf("(")) + 1).replace(");",
                "");
        log.info(data);
        JSONObject dataJo = JSON.parseObject(data);
        JSONObject resultJo = dataJo.getJSONObject("data");
        String token = resultJo.getString("token");
        log.info("------------token------------" + token);
        usrToken.put("token", token);
        String uuid = UUID.randomUUID().toString();
        mmm.put(uuid, usrToken);
        
        HttpSession session = request.getSession();
        session.setAttribute("token", token);
        session.setAttribute("tokenCookie", cookies);
        session.setAttribute("tracecode", retStrMap.get("tracecode"));
        session.setAttribute("P3P", retStrMap.get("P3P"));
        
        JSONObject retJo = new JSONObject();
        retJo.put("respCode", "0000");
        retJo.put("token", token);
        retJo.put("uuid", uuid);
        return retJo.toString();
    }
    
    /** 
     * @Description 调用js对百度账号的密码加密 
     * @param @param password
     * @param @return 参数 
     * @return String 返回类型  
     * @throws 
     */
    private String invokeJsForPwd(String password) {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");
        
        FileReader reader;
        String codepwd = "";
        try {
            reader = new FileReader(OrderController.class.getResource("")
                    .getFile() + File.separator + "code.js");
            engine.eval(reader);
            
            if (engine instanceof Invocable) {
                Invocable invoke = (Invocable)engine; // 调用merge方法，并传入两个参数    
                
                codepwd = (String)invoke.invokeFunction("h", password);
                
                log.info("-----------------password--------------->" + codepwd);
            }
            
            reader.close();
        }
        catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } // 执行指定脚本   
        catch (ScriptException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return codepwd;
    }
    
    @RequestMapping("/bdlogin")
    public @ResponseBody String bdlogin(HttpServletRequest request,
            @RequestBody String param) {
        
        JSONObject paramJo = JSON.parseObject(param);
        String code = paramJo.getString("code");
        String username = paramJo.getString("username");
        String password = paramJo.getString("password");
        String uuid = paramJo.getString("uuid");
        log.info("-----------password----------" + password);
        String codepwd = invokeJsForPwd(password);
        HttpSession session = request.getSession();
        String token = "";
        String tokenCookie = "";
        String tracecode = "";
        String P3P = "";
        //        if (JsonUtil.isNotBlank(session.getAttribute("token"))) {
        //            token = (String)session.getAttribute("token");
        //        }
        //        if (JsonUtil.isNotBlank(session.getAttribute("tokenCookie"))) {
        //            tokenCookie = (String)session.getAttribute("tokenCookie");
        //        }
        //        if (JsonUtil.isNotBlank(session.getAttribute("tracecode"))) {
        //            tracecode = (String)session.getAttribute("tracecode");
        //        }
        //        if (JsonUtil.isNotBlank(session.getAttribute("P3P"))) {
        //            P3P = (String)session.getAttribute("P3P");
        //        }
        Map<String, String> tokenParam = mmm.get(uuid);
        
        String queryStr = "redirect_url=https%253A%252F%252Fwmcrm.baidu.com%252F&return_url=https%253A%252F%252Fwmcrm.baidu.com%252Fcrm%252Fsetwmstoken&type=1&channel=pc&account="
                + username
                + "&upass="
                + codepwd
                + "&captcha="
                + code
                + "&token=" + tokenParam.get("token");
        Map<String, String> retStr = null;
        String queryOrderCookie = "";
        
        JSONObject retJo = new JSONObject();
        try {
            retStr = HttpsRequestUtil.bddoPost("https://wmpass.baidu.com/api/login",
                    queryStr,
                    "UTF-8",
                    300000,
                    300000,
                    tokenParam.get("tokenCookie"),
                    tokenParam.get("tracecode"),
                    tokenParam.get("P3P"));
            log.info(retStr.get("cookie"));
            log.info(retStr.get("result"));
            
            String WMSTOKEN = "";
            JSONObject loginJo = JSON.parseObject(retStr.get("result"));
            if (0 == loginJo.getInteger("errno")) {
                JSONObject dataJo = loginJo.getJSONObject("data");
                WMSTOKEN = dataJo.getString("WMSTOKEN");
                queryOrderCookie = tokenCookie
                        + retStr.get("cookie")
                        + "newyear=open;new_remind_time=1491707183;new_order_time=1491707474;"
                        + "WMSTOKEN=" + WMSTOKEN;
                retJo.put("respCode", "0000");
                retJo.put("queryOrderCookie", queryOrderCookie);
                retJo.put("respMsg", "登录成功！");
                insertToMongo(username, queryOrderCookie);
                return retJo.toString();
            }
            
            retJo.put("respCode", "9999");
            retJo.put("respMsg", loginJo.getString("errmsg"));
            
            //            Map<String, String> params = new HashMap<String, String>();
            //            Map<String, String> headers = new HashMap<String, String>();
            //            params.put("qt", "neworderlist");
            //            params.put("display", "json");
            //            headers.put("Cookie", tokenCookie + retStr.get("cookie"));
            //            log.info(queryOrderCookie);
            //            String orderInfo = HttpUtil.doGet("https://wmcrm.baidu.com/crm?hhs=secure&qt=neworderlist&display=json",
            //                    queryOrderCookie,
            //                    tracecode);
            //            log.info("orderinfo---------------------->" + orderInfo);
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return retJo.toString();
    }
    
    @RequestMapping("/login")
    public @ResponseBody String login(HttpServletRequest request,
            @RequestBody String param) {
        JSONObject resultJo = new JSONObject();
        JSONObject paramJo = JSON.parseObject(param);
        String retStr = HttpRequest.sendGet(UrlUtil.login + "?userName="
                + paramJo.getString("userName") + "&password="
                + paramJo.getString("password"));
        
        log.info("----------------获取到的参与者账号对应的外卖平台账号--------------------"
                + retStr);
        
        //        Map<String, String> baiduMap = new HashMap<String, String>();
        //        JSONObject retJo = JSON.parseObject(retStr);
        //        if ("0000".equals(retJo.getString("respCode"))) {
        //            JSONArray resultJa = retJo.getJSONArray("result");
        //            for (Object o : resultJa) {
        //                JSONObject eachJo = (JSONObject)o;
        //                if (eachJo.containsKey("baiduId")
        //                        && JsonUtil.isNotBlank(eachJo.get("baiduId"))) {
        //                    baiduMap.put(eachJo.getString("baiduId"),
        //                            eachJo.getString("baidupwd"));
        //                }
        //            }
        //            
        //            if (baiduMap.entrySet().size() > 0) {
        //                
        //                String code = paramJo.getString("code");
        //                String uuid = paramJo.getString("uuid");
        //                for (Map.Entry<String, String> entry : baiduMap.entrySet()) {
        //                    JSONObject entryJo = new JSONObject();
        //                    entryJo.put("username", entry.getKey());
        //                    entryJo.put("password", entry.getValue());
        //                    entryJo.put("code", code);
        //                    entryJo.put("uuid", uuid);
        //                    String loginInfo = bdlogin(request, entryJo.toString());
        //                    JSONObject loginJo = JSON.parseObject(loginInfo);
        //                    if ("0000".equals(loginJo.getString("respCode"))) {
        //                        insertToMongo(entry.getKey(),
        //                                loginJo.getString("queryOrderCookie"));
        //                    }
        //                    else {
        //                        resultJo.put("respCode", "9999");
        //                        resultJo.put("respMsg", loginJo.getString("respMsg"));
        //                        return resultJo.toString();
        //                    }
        //                }
        //            }
        //            
        //        }
        
        return retStr;
    }
    
    private void insertToMongo(String username, String loginInfo) {
        
        JSONObject info = new JSONObject();
        info.put("username", username);
        info.put("loginInfo", loginInfo);
        MongoTemplate mt = (MongoTemplate)SpringContextHolder.getWebApplicationContext()
                .getBean("mongoTemplate");
        DBCollection dbc = mt.getCollection("dn_bdloginInfo");
        BasicDBObject cond1 = new BasicDBObject();
        cond1.put("username", username);
        dbc.remove(cond1);
        mt.remove(cond1, "dn_bdloginInfo");
        mt.insert(info.toString(), "dn_bdloginInfo");
    }
    
    @RequestMapping("/searchOrder")
    public @ResponseBody String searchOrder(HttpServletRequest request,
            @RequestBody String param) {
        
        JSONObject paramJo = JSON.parseObject(param);
        
        //查询餐与者账号关联的所有外面平台的账号下面的所有订单
        List<String> elemshopIdsList = new ArrayList<String>();
        List<String> mtshopIdsList = new ArrayList<String>();
        List<String> bdwmshopIdsList = new ArrayList<String>();
        String shopIds = paramJo.getString("shopIds");
        JSONObject shopIdsJo = JSON.parseObject(shopIds);
        
        JSONArray orderJa = new JSONArray();
        
        if (shopIdsJo.containsKey("elemShops")) {
            JSONArray elemShopsJa = shopIdsJo.getJSONArray("elemShops");
            if (JsonUtil.isNotBlank(elemShopsJa) && elemShopsJa.size() > 0) {
                for (Object o : elemShopsJa) {
                    JSONObject shopIdJo = (JSONObject)o;
                    elemshopIdsList.add(shopIdJo.getString("shopId"));
                }
            }
        }
        
        if (shopIdsJo.containsKey("meituanShops")) {
            JSONArray meituanShopsJa = shopIdsJo.getJSONArray("meituanShops");
            if (JsonUtil.isNotBlank(meituanShopsJa)
                    && meituanShopsJa.size() > 0) {
                for (Object o : meituanShopsJa) {
                    JSONObject shopIdJo = (JSONObject)o;
                    mtshopIdsList.add(shopIdJo.getString("meituanId"));
                }
            }
        }
        
        if (shopIdsJo.containsKey("baiduShops")) {
            JSONArray baiduShopsJa = shopIdsJo.getJSONArray("baiduShops");
            if (JsonUtil.isNotBlank(baiduShopsJa) && baiduShopsJa.size() > 0) {
                for (Object o : baiduShopsJa) {
                    JSONObject shopIdJo = (JSONObject)o;
                    bdwmshopIdsList.add(shopIdJo.getString("baiduId"));
                }
            }
        }
        
        String platformType = paramJo.getString("platformType");
        if ("all".equals(platformType) || "elm".equals(platformType)) {
            if (elemshopIdsList.size() > 0) {
                for (String shopId : elemshopIdsList) {
                    JSONObject queryJo = elemQueryOrder(param, shopId);
                    if ("0000".equals(queryJo.getString("respCode"))) {
                        JSONArray ja = queryJo.getJSONArray("resultJa");
                        
                        for (Object oo : ja) {
                            orderJa.add(oo);
                        }
                    }
                }
            }
        }
        
        if ("all".equals(platformType) || "mt".equals(platformType)) {
            if (mtshopIdsList.size() > 0) {
                for (String shopId : mtshopIdsList) {
                    JSONObject queryJo = mtQueryOrder(param, shopId);
                    if ("0000".equals(queryJo.getString("respCode"))) {
                        JSONArray ja = queryJo.getJSONArray("resultJa");
                        
                        for (Object oo : ja) {
                            orderJa.add(oo);
                        }
                    }
                }
            }
        }
        
        if ("all".equals(platformType) || "bdwm".equals(platformType)) {
            if (bdwmshopIdsList.size() > 0) {
                for (String shopId : bdwmshopIdsList) {
                    JSONObject queryJo = bdwmQueryOrder(param, shopId);
                    if ("0000".equals(queryJo.getString("respCode"))) {
                        JSONArray ja = queryJo.getJSONArray("resultJa");
                        for (Object oo : ja) {
                            orderJa.add(oo);
                        }
                    }
                }
            }
        }
        
        JSONObject reJo = new JSONObject();
        reJo.put("respCode", "0000");
        reJo.put("respDesc", "查询成功！");
        reJo.put("result", orderJa);
        return reJo.toString();
    }
    
    private void handbdwdOrder(JSONArray orderJa, List<Map> result) {
        if (result == null || result.size() == 0) {
            return;
        }
        
        for (Map map : result) {
            orderJa.add(handEachJo(map));
        }
    }
    
    private JSONObject handEachJo(Map map) {
        JSONObject jo = new JSONObject();
        jo.put("shopName", map.get("store_name".toUpperCase()));
        jo.put("orderTime", map.get("order_time".toUpperCase()));
        jo.put("orderNo", map.get("order_no".toUpperCase()));
        jo.put("userName", map.get("consignee_name".toUpperCase()));
        jo.put("consigneeAddress", map.get("target_addr".toUpperCase()));
        
        jo.put("boxPrice", map.get("meal_fee".toUpperCase()));
        jo.put("distributionPrice",
                map.get("platform_dist_charge".toUpperCase()));
        jo.put("orderPrice", map.get("platform_dist_charge".toUpperCase()));
        return jo;
    }
    
    private JSONObject bdwmQueryOrder(String param, String username) {
        try {
            JSONObject paramJo = JSON.parseObject(param);
            BasicDBObject cond3 = new BasicDBObject();
            if (paramJo.containsKey("beginTime")) {
                BasicDBObject cond1 = new BasicDBObject();
                cond1.put("$gt", paramJo.getString("beginTime"));
                cond3.put("orderTime", cond1);
            }
            cond3.put("merchantId", username);
            MongoTemplate mt = (MongoTemplate)SpringContextHolder.getWebApplicationContext()
                    .getBean("mongoTemplate");
            DBCollection dbc = mt.getCollection("dn_order");
            DBCursor cursor = dbc.find(cond3);
            JSONArray resultJa = new JSONArray();
            while (cursor.hasNext()) {
                DBObject each = cursor.next();
                each.put("shopName", each.get("merchantName"));
                each.put("platformType", each.get("platform_type"));
                resultJa.add(each);
            }
            
            JSONObject resultJo = new JSONObject();
            resultJo.put("respCode", "0000");
            resultJo.put("resultJa", resultJa);
            return resultJo;
        }
        catch (Exception e) {
            e.printStackTrace();
            JSONObject reJo = new JSONObject();
            reJo.put("respCode", "9999");
            reJo.put("respDesc", e.getMessage());
            return reJo;
        }
    }
    
    private JSONObject mtQueryOrder(String param, String shopId) {
        try {
            JSONObject paramJo = JSON.parseObject(param);
            BasicDBObject cond3 = new BasicDBObject();
            if (paramJo.containsKey("beginTime")) {
                BasicDBObject cond1 = new BasicDBObject();
                cond1.put("$gt", paramJo.getString("beginTime"));
                cond3.put("orderTime", cond1);
            }
            cond3.put("merchantId", shopId);
            MongoTemplate mt = (MongoTemplate)SpringContextHolder.getWebApplicationContext()
                    .getBean("mongoTemplate");
            DBCollection dbc = mt.getCollection("dn_order");
            DBCursor cursor = dbc.find(cond3);
            JSONArray resultJa = new JSONArray();
            while (cursor.hasNext()) {
                DBObject each = cursor.next();
                each.put("shopName", each.get("merchantName"));
                each.put("platformType", each.get("platform_type"));
                resultJa.add(each);
            }
            
            JSONObject resultJo = new JSONObject();
            resultJo.put("respCode", "0000");
            resultJo.put("resultJa", resultJa);
            return resultJo;
        }
        catch (Exception e) {
            e.printStackTrace();
            JSONObject reJo = new JSONObject();
            reJo.put("respCode", "9999");
            reJo.put("respDesc", e.getMessage());
            return reJo;
        }
    }
    
    //    private JSONObject mtQueryOrder(String param, String shopId) {
    //        
    //        JSONObject paramJo = JSON.parseObject(param);
    //        MongoTemplate mt = (MongoTemplate)SpringContextHolder.getWebApplicationContext()
    //                .getBean("mongoTemplate");
    //        
    //        DBCollection dbc = mt.getCollection("dn_cookies");
    //        BasicDBObject cond1 = new BasicDBObject();
    //        cond1.put("userName", shopId);
    //        DBCursor cursor = dbc.find(cond1);
    //        
    //        String cookies = null;
    //        
    //        while (cursor.hasNext()) {
    //            DBObject dbo = cursor.next();
    //            cookies = dbo.get("cookies").toString();
    //        }
    //        
    //        String queryStr = "?getNewVo=1&wmOrderPayType=2&wmOrderStatus=-2&sortField=1&lastLabel=&nextLabel=&_token="
    //                + getValue(cookies, "token");
    //        
    //        if (paramJo.containsKey("beginTime")) {
    //            queryStr = queryStr + "&startDate="
    //                    + paramJo.getString("beginTime") + "&endDate="
    //                    + paramJo.getString("beginTime");
    //        }
    //        
    //        String result = HttpRequest.sendGet2(UrlUtil.mtQuery + queryStr,
    //                cookies);
    //        
    //        if (JsonUtil.isBlank(result)) {
    //            JSONObject retJo = new JSONObject();
    //            retJo.put("respCode", "9999");
    //            return retJo;
    //        }
    //        
    //        JSONObject retJo = null;
    //        if (isJson(result)) {
    //            retJo = JSON.parseObject(result);
    //        }
    //        else {
    //            JSONObject respJo = new JSONObject();
    //            respJo.put("respCode", "9999");
    //            return respJo;
    //        }
    //        
    //        if (JsonUtil.isBlank(retJo.get("wmOrderList"))) {
    //            JSONObject respJo = new JSONObject();
    //            respJo.put("respCode", "9999");
    //            return respJo;
    //        }
    //        
    //        JSONArray orderListJa = retJo.getJSONArray("wmOrderList");
    //        
    //        JSONArray orderList = new JSONArray();
    //        JSONObject orderJo = new JSONObject();
    //        
    //        for (Object o : orderListJa) {
    //            JSONObject jo = (JSONObject)o;
    //            orderList.add(fixDatamt(jo, shopId));
    //        }
    //        
    //        orderJo.put("respCode", "0000");
    //        orderJo.put("orderList", orderList);
    //        
    //        return orderJo;
    //    }
    
    private boolean isJson(String param) {
        try {
            JSON.parseObject(param);
        }
        catch (Exception e) {
            return false;
        }
        return true;
    }
    
    //    private JSONObject elemQueryOrder(String param, String shopId) {
    //        JSONObject paramJo = JSON.parseObject(param);
    //        MongoTemplate mt = (MongoTemplate)SpringContextHolder.getWebApplicationContext()
    //                .getBean("mongoTemplate");
    //        
    //        DBCollection dbc = mt.getCollection("dn_ksid");
    //        BasicDBObject cond1 = new BasicDBObject();
    //        cond1.put("shopId", shopId);
    //        DBCursor cursor = dbc.find(cond1);
    //        
    //        String ksid = null;
    //        
    //        while (cursor.hasNext()) {
    //            DBObject dbo = cursor.next();
    //            ksid = dbo.get("ksid").toString();
    //        }
    //        
    //        JSONObject queryOrderJo = ElemeUtil.queryOrderJo();
    //        queryOrderJo.getJSONObject("metas").put("ksid", ksid);
    //        
    //        queryOrderJo.getJSONObject("params").put("shopId", shopId);
    //        queryOrderJo.getJSONObject("params")
    //                .put("orderFilter",
    //                        ElemeUtil.orderFilterMap.get(paramJo.getString("orderLatestStatus")));
    //        
    //        if (paramJo.containsKey("beginTime")) {
    //            queryOrderJo.getJSONObject("params")
    //                    .getJSONObject("condition")
    //                    .put("beginTime",
    //                            paramJo.getString("beginTime") + "T00:00:00");
    //            queryOrderJo.getJSONObject("params")
    //                    .getJSONObject("condition")
    //                    .put("endTime",
    //                            paramJo.getString("beginTime") + "T23:59:59");
    //        }
    //        
    //        log.info("--------------queryOrderJo-------------"
    //                + queryOrderJo.toString());
    //        
    //        try {
    //            String queryOrderRe = HttpsRequestUtil.doPost(ElemeUtil.queryOrderurl,
    //                    queryOrderJo.toString(),
    //                    "UTF-8",
    //                    300000,
    //                    300000);
    //            
    //            log.info("--------------------queryOrderurl-----------------"
    //                    + queryOrderRe);
    //            JSONObject queryReJo = JSON.parseObject(queryOrderRe);
    //            JSONArray reJa = queryReJo.getJSONArray("result");
    //            
    //            if (JsonUtil.isBlank(reJa)) {
    //                JSONObject retJo = new JSONObject();
    //                retJo.put("respCode", "9999");
    //                return retJo;
    //            }
    //            JSONArray retJa = new JSONArray();
    //            for (Object o : reJa) {
    //                JSONObject reJo = (JSONObject)o;
    //                retJa.add(fixData(reJo));
    //            }
    //            JSONObject retJo = new JSONObject();
    //            retJo.put("respCode", "0000");
    //            retJo.put("retJa", retJa);
    //            return retJo;
    //        }
    //        catch (Exception e) {
    //            JSONObject retJo = new JSONObject();
    //            retJo.put("respCode", "9999");
    //            e.printStackTrace();
    //            return retJo;
    //        }
    //    }
    
    private JSONObject elemQueryOrder(String param, String shopId) {
        try {
            JSONObject paramJo = JSON.parseObject(param);
            BasicDBObject cond3 = new BasicDBObject();
            if (paramJo.containsKey("beginTime")) {
                BasicDBObject cond1 = new BasicDBObject();
                cond1.put("$gt", paramJo.getString("beginTime"));
                cond3.put("orderTime", cond1);
            }
            cond3.put("merchantId", shopId);
            MongoTemplate mt = (MongoTemplate)SpringContextHolder.getWebApplicationContext()
                    .getBean("mongoTemplate");
            DBCollection dbc = mt.getCollection("dn_order");
            DBCursor cursor = dbc.find(cond3);
            JSONArray resultJa = new JSONArray();
            while (cursor.hasNext()) {
                DBObject each = cursor.next();
                each.put("shopName", each.get("merchantName"));
                each.put("platformType", each.get("platform_type"));
                resultJa.add(each);
            }
            
            JSONObject resultJo = new JSONObject();
            resultJo.put("respCode", "0000");
            resultJo.put("resultJa", resultJa);
            return resultJo;
        }
        catch (Exception e) {
            e.printStackTrace();
            JSONObject reJo = new JSONObject();
            reJo.put("respCode", "9999");
            reJo.put("respDesc", e.getMessage());
            return reJo;
        }
    }
    
    private String dateParse(long mesc) {
        SimpleDateFormat dateformat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss");
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(mesc);
        return dateformat.format(gc.getTime());
    }
    
    private JSONObject fixDatamt(JSONObject jo, String userName) {
        JSONObject fixJo = new JSONObject();
        fixJo.put("orderTime",
                dateParse(Long.parseLong(JsonUtil.getLong(jo, "order_time")
                        + "000")));
        fixJo.put("orderNo", JsonUtil.getString(jo, "wm_order_id_view_str"));
        fixJo.put("userName", JsonUtil.getString(jo, "recipient_name"));
        fixJo.put("phone", JsonUtil.getString(jo, "recipient_phone"));
        fixJo.put("merchantId", userName);
        
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
                        dishJo.put("goods_price", JsonUtil.getString(detailJo,
                                "origin_food_price"));
                        dishesJa.add(dishJo);
                    }
                }
            }
            fixJo.put("dishes", dishesJa);
        }
        fixJo.put("boxPrice", JsonUtil.getString(jo, "boxPriceTotal"));
        fixJo.put("orderPrice", JsonUtil.getString(jo, "total_before"));
        fixJo.put("state", "0");
        fixJo.put("merchantName", JsonUtil.getString(jo, "poi_name"));
        fixJo.put("platform_type", "mt");
        fixJo.put("consignee_name", JsonUtil.getString(jo, "recipient_name"));
        fixJo.put("consigneeAddress",
                JsonUtil.getString(jo, "recipient_address"));
        fixJo.put("distance", JsonUtil.getString(jo, "distance"));
        fixJo.put("remark", JsonUtil.getString(jo, "remark"));
        fixJo.put("shopName", JsonUtil.getString(jo, "poi_name"));
        
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
        fixJo.put("activities_subsidy_bymerchant", jo.getDouble("total_before")
                - jo.getDouble("total_after"));
        
        fixJo.put("orderLatestStatus",
                JsonUtil.getString(jo, "orderLatestStatus"));
        return fixJo;
    }
    
    private JSONObject fixData(JSONObject jo) {
        JSONObject fixJo = new JSONObject();
        
        fixJo.put("respCode", "0000");
        fixJo.put("orderTime", JsonUtil.getString(jo, "activeTime"));
        fixJo.put("orderNo", JsonUtil.getString(jo, "id"));
        fixJo.put("userName", JsonUtil.getString(jo, "consigneeName"));
        //        fixJo.put("sex", JsonUtil.getString(fixJo, "consigneeName"));
        fixJo.put("phone", jo.getJSONArray("consigneePhones").get(0));
        fixJo.put("merchantId", JsonUtil.getString(jo, "shopId"));
        
        JSONArray groupsJa = jo.getJSONArray("groups");
        JSONArray dishesJa = new JSONArray();
        for (Object o : groupsJa) {
            JSONObject groupsJo = (JSONObject)o;
            if ("NORMAL".equals(groupsJo.getString("type"))) {
                JSONArray itemsJa = groupsJo.getJSONArray("items");
                
                for (Object itemO : itemsJa) {
                    JSONObject itemJo = (JSONObject)itemO;
                    JSONObject dishesJo = new JSONObject();
                    dishesJo.put("dishName", JsonUtil.getString(itemJo, "name"));
                    dishesJo.put("activityName", "特价");
                    dishesJo.put("count",
                            JsonUtil.getString(itemJo, "quantity"));
                    dishesJo.put("price1", JsonUtil.getString(itemJo, "price"));
                    dishesJo.put("price2", JsonUtil.getString(itemJo, "total"));
                    dishesJo.put("goods_id", JsonUtil.getString(itemJo, "id"));
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
        fixJo.put("orderType", JsonUtil.getString(jo, "orderType"));
        fixJo.put("merchantActivityPart",
                JsonUtil.getString(jo, "merchantActivityPart"));
        fixJo.put("elemeActivityPart",
                JsonUtil.getString(jo, "elemeActivityPart"));
        fixJo.put("serviceFee", JsonUtil.getString(jo, "serviceFee"));
        fixJo.put("serviceRate", JsonUtil.getString(jo, "serviceRate"));
        
        fixJo.put("platform_dist_charge",
                JsonUtil.getString(jo, "deliveryFeeTotal"));
        fixJo.put("settlement_amount", JsonUtil.getString(jo, "income"));
        fixJo.put("distribution_mode",
                JsonUtil.getString(jo, "deliveryServiceType"));
        fixJo.put("remark", JsonUtil.getString(jo, "remark"));
        fixJo.put("platform_type", "elm");
        fixJo.put("booked_time", JsonUtil.getString(jo, "bookedTime"));
        fixJo.put("consignee_name", JsonUtil.getString(jo, "consigneeName"));
        fixJo.put("active_time", JsonUtil.getString(jo, "activeTime"));
        fixJo.put("active_total", JsonUtil.getString(jo, "activityTotal"));
        fixJo.put("times", JsonUtil.getString(jo, "times"));
        fixJo.put("consigneeAddress",
                JsonUtil.getString(jo, "consigneeAddress"));
        fixJo.put("distance", JsonUtil.getString(jo, "distance"));
        fixJo.put("goodsSummary", JsonUtil.getString(jo, "goodsSummary"));
        fixJo.put("platformType", "elm");
        fixJo.put("shopName", JsonUtil.getString(jo, "shopName"));
        fixJo.put("orderLatestStatus",
                JsonUtil.getString(jo, "orderLatestStatus"));
        return fixJo;
    }
    
    @RequestMapping("/loadOrder")
    public @ResponseBody String loadOrder(HttpServletRequest request,
            @RequestBody String param) {
        
        JSONObject paramJo = JSON.parseObject(param);
        String channelType = JsonUtil.getString(paramJo, "channelType");
        
        log.info("<----------------------渠道编码：------------------------>"
                + channelType);
        
        String confirmStr = confirmOrder(request, param);
        JSONObject confirmJo = JSON.parseObject(confirmStr);
        if ("9999".equals(confirmJo.getString("respCode"))) {
            updateMongodbState(param);
            return confirmJo.toString();
        }
        
        JSONObject updateStateRet = insertToMongo(paramJo);
        
        if ("9999".equals(updateStateRet.getString("respCode"))) {
            return updateStateRet.toString();
        }
        
        String insertDb = orderInsertDb(request, param);
        JSONObject insertDbJo = JSON.parseObject(insertDb);
        if ("9999".equals(insertDbJo.getString("respCode"))) {
            return insertDbJo.toString();
        }
        if ("PC".equals(channelType)) {
            print();
        }
        
        return confirmJo.toString();
    }
    
    private JSONObject insertToMongo(JSONObject queryReJo) {
        try {
            MongoTemplate mt = (MongoTemplate)SpringContextHolder.getWebApplicationContext()
                    .getBean("mongoTemplate");
            
            //            DBCollection dbc = mt.getCollection("dn_order");
            //            BasicDBObject cond1 = new BasicDBObject();
            //            cond1.put("orderNo", JsonUtil.getString(queryReJo, "id"));
            //            DBCursor cursor = dbc.find(cond1);
            //            if (!cursor.hasNext()) {
            mt.insert(queryReJo.toString(), "dn_order");
            //            }
            JSONObject retJo = new JSONObject();
            retJo.put("respCode", "0000");
            retJo.put("respDesc", "接单成功！");
            return retJo;
        }
        catch (Exception e) {
            e.printStackTrace();
            JSONObject retJo = new JSONObject();
            retJo.put("respCode", "9999");
            retJo.put("respDesc", "接单失败！");
            return retJo;
        }
    }
    
    private void print() {
        int height = 175 + 3 * 15 + 20;
        
        // 通俗理解就是书、文档  
        Book book = new Book();
        
        // 打印格式  
        PageFormat pf = new PageFormat();
        pf.setOrientation(PageFormat.PORTRAIT);
        
        // 通过Paper设置页面的空白边距和可打印区域。必须与实际打印纸张大小相符。  
        Paper p = new Paper();
        p.setSize(230, height);
        p.setImageableArea(5, -20, 230, height + 20);
        pf.setPaper(p);
        
        // 把 PageFormat 和 Printable 添加到书中，组成一个页面  
        book.append(new Prient(), pf);
        
        // 获取打印服务对象  
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPageable(book);
        try {
            job.print();
        }
        catch (PrinterException e) {
            e.printStackTrace();
            log.info("================打印出现异常");
        }
    }
    
    private JSONObject updateMongodbState(String param) {
        try {
            JSONObject paramJo = JSON.parseObject(param);
            
            DBCollection dbc = mt.getCollection("dn_order");
            BasicDBObject cond2 = new BasicDBObject();
            cond2.put("merchantId", JsonUtil.getString(paramJo, "merchantId"));
            cond2.put("orderNo", JsonUtil.getString(paramJo, "orderNo"));
            cond2.put("state", "0");
            
            BasicDBObject cond3 = new BasicDBObject();
            BasicDBObject cond4 = new BasicDBObject();
            cond4.put("state", "1");
            cond3.put("$set", cond4);
            dbc.updateMulti(cond2, cond3);
            
            JSONObject retJo = new JSONObject();
            retJo.put("respCode", "0000");
            retJo.put("respDesc", "接单成功！");
            return retJo;
        }
        catch (Exception e) {
            e.printStackTrace();
            JSONObject retJo = new JSONObject();
            retJo.put("respCode", "9999");
            retJo.put("respDesc", "接单失败！");
            return retJo;
        }
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
    
    @RequestMapping("/orderInsertBatchDb")
    public @ResponseBody String orderInsertBatchDb(HttpServletRequest request,
            @RequestBody String param) {
        try {
            return os.saveOrders(param);
        }
        catch (Exception e) {
            e.printStackTrace();
            JSONObject reJo = new JSONObject();
            reJo.put("respCode", "9999");
            reJo.put("respDesc", e.getMessage());
            return reJo.toString();
        }
    }
    
    private String elemConfireOrder(JSONObject paramJo) {
        MongoTemplate mt = (MongoTemplate)SpringContextHolder.getWebApplicationContext()
                .getBean("mongoTemplate");
        
        DBCollection dbc = mt.getCollection("dn_ksid");
        BasicDBObject cond1 = new BasicDBObject();
        cond1.put("shopId", JsonUtil.getString(paramJo, "merchantId"));
        DBCursor cursor = dbc.find(cond1);
        
        String ksid = null;
        
        while (cursor.hasNext()) {
            DBObject dbo = cursor.next();
            ksid = dbo.get("ksid").toString();
        }
        
        JSONObject confirmOrderJo = ElemeUtil.confirmOrderJo();
        confirmOrderJo.getJSONObject("metas").put("ksid", ksid);
        confirmOrderJo.getJSONObject("params").put("orderId",
                JsonUtil.getString(paramJo, "orderNo"));
        
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
            retJo.put("respDesc", "接单成功！");
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
    
    private String mtConfirmOrder(JSONObject paramJo) {
        
        try {
            MongoTemplate mt = (MongoTemplate)SpringContextHolder.getWebApplicationContext()
                    .getBean("mongoTemplate");
            
            DBCollection dbc = mt.getCollection("dn_cookies");
            BasicDBObject cond1 = new BasicDBObject();
            cond1.put("userName", JsonUtil.getString(paramJo, "merchantId"));
            DBCursor cursor = dbc.find(cond1);
            
            String cookies = null;
            
            while (cursor.hasNext()) {
                DBObject dbo = cursor.next();
                cookies = dbo.get("cookies").toString();
            }
            
            Map<String, String> retMap = HttpRequest.sendPost1(UrlUtil.mtconfirm,
                    "wmPoiId=" + getValue(cookies, "wmPoiId") + "&orderId="
                            + JsonUtil.getString(paramJo, "orderNo")
                            + "&acctId=" + getValue(cookies, "acctId")
                            + "&appType=3&token=" + getValue(cookies, "token")
                            + "&isPrint=0&isAutoAccept=0&csrfToken=",
                    cookies);
            log.info("--------------mtConfirmOrder--result-----------------"
                    + retMap);
            String result = retMap.get("result");
            JSONObject resultJo = JSON.parseObject(result);
            if (JsonUtil.isNotBlank(resultJo.get("data"))) {
                if (resultJo.containsKey("code")
                        && "0".equals(resultJo.getString("code"))) {
                    JSONObject retJo = new JSONObject();
                    retJo.put("respCode", "0000");
                    retJo.put("respDesc", "接单成功！");
                    return retJo.toString();
                }
            }
            
            JSONObject reJo = new JSONObject();
            reJo.put("respCode", "9999");
            reJo.put("respDesc", "接单失败！");
            return reJo.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
            JSONObject reJo = new JSONObject();
            reJo.put("respCode", "9999");
            reJo.put("respDesc", e.getMessage());
            return reJo.toString();
        }
    }
    
    private String bdwmConfirmOrder(JSONObject paramJo) {
        try {
            MongoTemplate mt = (MongoTemplate)SpringContextHolder.getWebApplicationContext()
                    .getBean("mongoTemplate");
            
            DBCollection dbc = mt.getCollection("dn_bdloginInfo");
            BasicDBObject cond1 = new BasicDBObject();
            cond1.put("username", JsonUtil.getString(paramJo, "merchantId"));
            DBCursor cursor = dbc.find(cond1);
            
            String cookies = null;
            
            while (cursor.hasNext()) {
                DBObject dbo = cursor.next();
                cookies = dbo.get("loginInfo").toString();
            }
            
            Map<String, String> retMap = HttpRequest.sendPost1(UrlUtil.bdwmconfirm,
                    "order_id=" + JsonUtil.getString(paramJo, "orderNo")
                            + "&pc_ver=4.1.0&from=pc-ke",
                    cookies);
            log.info("--------------bdwmConfirmOrder--result-----------------"
                    + retMap);
            String result = retMap.get("result");
            JSONObject resultJo = JSON.parseObject(result);
            if (0 == resultJo.getInteger("errno")) {
                JSONObject retJo = new JSONObject();
                retJo.put("respCode", "0000");
                retJo.put("respDesc", "接单成功！");
                return retJo.toString();
            }
            
            JSONObject reJo = new JSONObject();
            reJo.put("respCode", "9999");
            reJo.put("respDesc", "接单失败！");
            return reJo.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
            JSONObject reJo = new JSONObject();
            reJo.put("respCode", "9999");
            reJo.put("respDesc", e.getMessage());
            return reJo.toString();
        }
    }
    
    private String getValue(String cookies, String name) {
        
        if (JsonUtil.isBlank(cookies)) {
            return "";
        }
        
        String[] cookiesArr = cookies.split(";");
        
        for (String each : cookiesArr) {
            if (each.contains(name)) {
                return each.replace(name + "=", "");
            }
        }
        
        return null;
    }
    
    @RequestMapping("/confirmOrder")
    public @ResponseBody String confirmOrder(HttpServletRequest request,
            @RequestBody String param) {
        JSONObject paramJo = JSON.parseObject(param);
        
        if ("elm".equals(paramJo.getString("platform_type"))) {
            return elemConfireOrder(paramJo);
        }
        else if ("mt".equals(paramJo.getString("platform_type"))) {
            return mtConfirmOrder(paramJo);
        }
        else if ("bdwm".equals(paramJo.getString("platform_type"))) {
            return bdwmConfirmOrder(paramJo);
        }
        
        return null;
    }
    
    @RequestMapping("/queryOrder")
    public @ResponseBody String queryOrder(HttpServletRequest request,
            @RequestBody String param) {
        try {
            return os.queryOrder(param).toString();
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
    public static void main(String[] args) {
        //        File directory = new File("");
        //        try {
        //            log.info(directory.getCanonicalPath());
        //            //            System.out.println(Thread.currentThread()
        //            //                    .getContextClassLoader()
        //            //                    .getResource(""));
        //            //            System.out.println(OrderController.class.getClassLoader()
        //            //                    .getResource(""));
        //            //            System.out.println(ClassLoader.getSystemResource(""));
        //            System.out.println(OrderController.class.getResource("").getFile());
        //            FileReader reader = new FileReader(
        //                    OrderController.class.getResource("").getFile()
        //                            + File.separator + "code.js");
        //            //            System.out.println(OrderController.class.getResource("/"));//Class文件所在路径 
        //            //            System.out.println(new File("/").getAbsolutePath());
        //            //            System.out.println(System.getProperty("user.dir"));
        //            System.out.print(reader);
        //        }
        //        catch (IOException e) {
        //            // TODO Auto-generated catch block
        //            e.printStackTrace();
        //        }
        JSONObject jo = new JSONObject();
        jo.put("username", "15243688033_eleme");
        jo.put("password", "dbb888888");
        new OrderController().elmUserLogin(jo);
    }
}
