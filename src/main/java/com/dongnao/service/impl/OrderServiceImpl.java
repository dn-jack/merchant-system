package com.dongnao.service.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dongnao.bean.AccountOrderDetail;
import com.dongnao.bean.AccountSaleGoods;
import com.dongnao.dao.OrderMapper;
import com.dongnao.util.JsonUtil;

@Service
public class OrderServiceImpl implements OrderService {
    
    @Autowired
    OrderMapper om;
    
    public String saveOrder(String param) throws Exception {
        JSONObject paramJo = JSON.parseObject(param);
        AccountOrderDetail aod = new AccountOrderDetail();
        
        String booktime = JsonUtil.getString(paramJo, "booked_time");
        if (booktime.contains("T")) {
            booktime = booktime.replace("T", " ");
        }
        
        String active_time = JsonUtil.getString(paramJo, "active_time");
        if (active_time.contains("T")) {
            active_time = active_time.replace("T", " ");
        }
        
        String platform_type = JsonUtil.getString(paramJo, "platform_type");
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        aod.setCreateDate(sdf.format(new Date()));
        aod.setStoreId(JsonUtil.getString(paramJo, "merchantId"));
        if ("elm".equals(platform_type)) {
            aod.setStoreELMId(JsonUtil.getString(paramJo, "merchantId"));
        }
        else if ("mt".equals(platform_type)) {
            aod.setStoreMTId(JsonUtil.getString(paramJo, "merchantId"));
        }
        else if ("bdwm".equals(platform_type)) {
            aod.setStoreBDId(JsonUtil.getString(paramJo, "merchantId"));
        }
        aod.setOrderTime(JsonUtil.getString(paramJo, "orderTime"));
        aod.setStoreName(JsonUtil.getString(paramJo, "merchantName"));
        aod.setOrderType(JsonUtil.getString(paramJo, "orderType"));
        aod.setOrderTime(active_time);
        aod.setOrderNo(JsonUtil.getString(paramJo, "orderNo"));
        aod.setPrices(JsonUtil.getBigDecimal(paramJo, "orderPrice"));
        aod.setOrginPrice(JsonUtil.getBigDecimal(paramJo, "orderPrice"));
        aod.setMealFee(JsonUtil.getBigDecimal(paramJo, "boxPrice"));
        aod.setMerchantActivitiesSubsidies(JsonUtil.getBigDecimal(paramJo,
                "merchantActivityPart"));
        aod.setPlatformActivitiesSubsidies(JsonUtil.getBigDecimal(paramJo,
                "elemeActivityPart"));
        aod.setServiceCharge(JsonUtil.getBigDecimal(paramJo, "serviceFee"));
        aod.setServiceRate(JsonUtil.getString(paramJo, "serviceRate"));
        
        aod.setPlatformDistCharge(JsonUtil.getBigDecimal(paramJo,
                "platform_dist_charge"));
        
        aod.setMerchantDistCharge(JsonUtil.getBigDecimal(paramJo,
                "merchant_dist_charge"));
        aod.setSettlementAmount(JsonUtil.getBigDecimal(paramJo,
                "settlement_amount"));
        aod.setDistributionMode(JsonUtil.getString(paramJo, "distribution_mode"));
        aod.setRemark(JsonUtil.getString(paramJo, "remark"));
        aod.setPlatformType(JsonUtil.getString(paramJo, "platform_type"));
        aod.setBookedTime(booktime);
        aod.setConsigneeName(JsonUtil.getString(paramJo, "consignee_name"));
        aod.setActiveTime(active_time);
        aod.setActiveTotal(JsonUtil.getBigDecimal(paramJo, "active_total"));
        aod.setActivitiesSubsidyBymerchant(JsonUtil.getBigDecimal(paramJo,
                "activities_subsidy_bymerchant"));
        aod.setConsigneeAddress(JsonUtil.getString(paramJo, "consigneeAddress"));
        
        StringBuffer goodsName = new StringBuffer();
        StringBuffer goods_quality = new StringBuffer();
        StringBuffer goods_id = new StringBuffer();
        StringBuffer goods_price = new StringBuffer();
        JSONArray dishesJa = null;
        if (paramJo.containsKey("dishes")) {
            dishesJa = paramJo.getJSONArray("dishes");
            for (Object o : dishesJa) {
                JSONObject dishesJo = (JSONObject)o;
                goodsName.append(JsonUtil.getString(dishesJo, "dishName"))
                        .append(",");
                goods_quality.append(JsonUtil.getString(dishesJo, "count"))
                        .append(",");
                goods_id.append(JsonUtil.getString(dishesJo, "goods_id"))
                        .append(",");
                goods_price.append(JsonUtil.getString(dishesJo, "goods_price"))
                        .append(",");
            }
            aod.setGoodsName(goodsName.substring(0, goodsName.lastIndexOf(",")));
            aod.setGoodsQuality(goods_quality.substring(0,
                    goods_quality.lastIndexOf(",")));
            aod.setGoodsId(goods_id.substring(0, goods_id.lastIndexOf(",")));
            aod.setGoodsPrice(goods_price.substring(0,
                    goods_price.lastIndexOf(",")));
        }
        
        int count = om.saveOrder(aod);
        if (count > 0 && dishesJa != null) {
            saveDishes(dishesJa,
                    JsonUtil.getString(paramJo, "merchantId"),
                    JsonUtil.getString(paramJo, "merchantName"),
                    JsonUtil.getString(paramJo, "orderNo"),
                    platform_type);
        }
        JSONObject reJo = new JSONObject();
        if (count > 0) {
            reJo.put("respCode", "0000");
            reJo.put("respDesc", "接单成功！");
        }
        else {
            reJo.put("respCode", "9999");
            reJo.put("respDesc", "接单失败！");
        }
        return reJo.toString();
    }
    
    public String saveDishes(JSONArray param, String storeId, String storeName,
            String orderNo, String platformType) throws Exception {
        for (Object o : param) {
            JSONObject dishesJo = (JSONObject)o;
            AccountSaleGoods good = new AccountSaleGoods();
            good.setStoreId(storeId);
            good.setStoreName(storeName);
            good.setOrderNo(orderNo);
            good.setGoodName(JsonUtil.getString(dishesJo, "dishName"));
            good.setGoodNum(JsonUtil.getInteger(dishesJo, "count"));
            good.setPlatformType(platformType);
            good.setGoodUnitPrice(JsonUtil.getString(dishesJo, "goods_price"));
            om.saveDishes(good);
        }
        
        return null;
    }
    
    public JSONObject queryOrder(String param, String username)
            throws Exception {
        Map queryMap = new HashMap();
        queryMap.put("username", username);
        JSONObject paramJo = JSON.parseObject(param);
        if (paramJo.containsKey("beginTime")) {
            queryMap.put("beginTime", paramJo.getString("beginTime"));
        }
        List<Map> results = om.queryOrder(queryMap);
        
        JSONObject ret = new JSONObject();
        ret.put("respCode", "0000");
        ret.put("respDesc", "查询成功！");
        ret.put("result", results);
        return ret;
    }
}
