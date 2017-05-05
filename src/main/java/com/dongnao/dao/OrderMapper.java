package com.dongnao.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.dongnao.bean.AccountOrderDetail;
import com.dongnao.bean.AccountSaleGoods;

@Repository
public interface OrderMapper {
    int saveOrder(AccountOrderDetail detail);
    
    int saveOrders(@Param("list") List<AccountOrderDetail> details);
    
    List<Map> queryOrder(Map param);
    
    int saveDishes(AccountSaleGoods goods);
    
    int saveDishess(@Param("list") List<AccountSaleGoods> goods);
}
