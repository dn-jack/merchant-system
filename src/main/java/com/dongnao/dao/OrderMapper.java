package com.dongnao.dao;

import org.springframework.stereotype.Repository;

import com.dongnao.bean.AccountOrderDetail;

@Repository
public interface OrderMapper {
    int saveOrder(AccountOrderDetail detail);
}
