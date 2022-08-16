package com.atguigu.service;

import com.atguigu.entity.OrderInfo;
import com.atguigu.enums.ProcessStatus;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 订单表 订单表 服务类
 * </p>
 *
 * @author GodWei
 * @since 2022-08-09
 */
public interface OrderInfoService extends IService<OrderInfo> {

    String generateTradeNo(String userId);

    boolean compareTradeNo(String userId, String tradeNoUI);

    Long saveOrderAndDetail(OrderInfo orderInfo);

    void deleteTradeNo(String userId);

    OrderInfo getOrderInfo(Long orderId);


    void updateOrderStatusByProcessStatus(OrderInfo orderInfo, ProcessStatus paid);

    void sendMessageToWareHouse(OrderInfo orderInfo);

    String splitOrder(Long orderId, String wareHouseIdSkuIdMapJson);

    String checkStockAndPrice(String userId, OrderInfo orderInfo);
}
