package com.dongnao.websocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.log4j.Logger;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dongnao.util.JsonUtil;
import com.dongnao.util.SpringUtil;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

@ServerEndpoint("/push")
public class PushOrder {
    
    private static Logger log = Logger.getLogger(PushOrder.class);
    
    public PushOrder() {
        log.info("PushOrder");
    }
    
    @OnOpen
    public void onopen(Session session, EndpointConfig config) {
        log.info("连接成功");
        
    }
    
    @OnError
    public void onError(Throwable t) {
        t.printStackTrace();
    }
    
    @OnClose
    public void onclose(Session session) {
        log.info("close....");
        
    }
    
    @OnMessage
    public void onsend(Session session, String msg) {
        log.info("sessionId : " + session.getId());
        JSONObject jo = JSON.parseObject(msg);
        
        List<String> shopIdsList = new ArrayList<String>();
        
        String shopIds = jo.getString("shopIds");
        JSONObject shopIdsJo = JSON.parseObject(shopIds);
        
        if (shopIdsJo.containsKey("elemShops")) {
            JSONArray elemShopsJa = shopIdsJo.getJSONArray("elemShops");
            if (JsonUtil.isNotBlank(elemShopsJa) && elemShopsJa.size() > 0) {
                for (Object o : elemShopsJa) {
                    String shopId = (String)o;
                    shopIdsList.add(shopId);
                }
            }
        }
        
        if (shopIdsJo.containsKey("meituanShops")) {
            JSONArray meituanShopsJa = shopIdsJo.getJSONArray("meituanShops");
            if (JsonUtil.isNotBlank(meituanShopsJa)
                    && meituanShopsJa.size() > 0) {
                for (Object o : meituanShopsJa) {
                    String shopId = (String)o;
                    shopIdsList.add(shopId);
                }
            }
        }
        
        if (shopIdsJo.containsKey("baiduShops")) {
            JSONArray baiduShopsJa = shopIdsJo.getJSONArray("baiduShops");
            if (JsonUtil.isNotBlank(baiduShopsJa) && baiduShopsJa.size() > 0) {
                for (Object o : baiduShopsJa) {
                    String shopId = (String)o;
                    shopIdsList.add(shopId);
                }
            }
        }
        
        Object[] shopIdsArr = shopIdsList.size() > 0 ? shopIdsList.toArray()
                : new String[] {};
        
        pullOrderThread pullOrder = new pullOrderThread(shopIdsArr, session);
        
        new Thread(pullOrder).start();
    }
    
    public class pullOrderThread implements Runnable {
        
        private Object[] merchantIds;
        
        private Session session;
        
        public boolean flag = true;
        
        public pullOrderThread(Object[] merchantIds, Session session) {
            this.merchantIds = merchantIds;
            this.session = session;
        }
        
        public boolean isFlag() {
            return flag;
        }
        
        public void setFlag(boolean flag) {
            this.flag = flag;
        }
        
        public void run() {
            
            BasicDBList dblist = new BasicDBList();
            for (Object merchantId : merchantIds) {
                dblist.add(merchantId);
            }
            
            while (flag) {
                MongoTemplate mt = (MongoTemplate)SpringUtil.getApplicationContext()
                        .getBean("mongoTemplate");
                
                DBCollection dbc = mt.getCollection("dn_order");
                BasicDBObject cond1 = new BasicDBObject();
                BasicDBObject cond2 = new BasicDBObject();
                
                cond2.put("$in", dblist);
                cond1.put("merchantId", cond2);
                cond1.put("state", "0");
                
                DBCursor cursor = dbc.find(cond1);
                
                List<DBObject> orders = new ArrayList<DBObject>();
                
                while (cursor.hasNext()) {
                    DBObject dbo = cursor.next();
                    orders.add(dbo);
                }
                
                //            List<String> orders = mt.find(new Query(
                //                    new Criteria("merchantId").is(jo.getString("merchantId"))),
                //                    String.class);
                
                try {
                    if (session.isOpen()) {
                        if (orders.size() > 0)
                            session.getBasicRemote()
                                    .sendText(JSONArray.toJSONString(orders));
                    }
                    else {
                        flag = false;
                    }
                    
                    Thread.sleep(5000);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
            }
        }
    }
}
