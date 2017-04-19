package com.dongnao.service.impl;

import com.alibaba.fastjson.JSONObject;

public interface OrderService {
    String saveOrder(String param) throws Exception;
    
    JSONObject queryOrder(String param, String username) throws Exception;
}
