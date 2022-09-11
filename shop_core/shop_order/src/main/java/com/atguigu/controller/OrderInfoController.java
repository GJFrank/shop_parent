package com.atguigu.controller;


import com.atguigu.client.CartFeignClient;
import com.atguigu.client.UserFeignClient;
import com.atguigu.entity.CartInfo;
import com.atguigu.entity.OrderDetail;
import com.atguigu.entity.OrderInfo;
import com.atguigu.entity.UserAddress;
import com.atguigu.result.RetVal;
import com.atguigu.service.OrderDetailService;
import com.atguigu.service.OrderInfoService;
import com.atguigu.util.AuthContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.netflix.ribbon.proxy.annotation.Http;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 订单表 订单表 前端控制器
 * </p>
 *
 * @author GodWei
 * @since 2022-08-09
 */
@RestController
@RequestMapping("/order")
public class OrderInfoController {
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private CartFeignClient cartFeignClient;
    @Autowired
    private OrderInfoService orderInfoService;
    @Autowired
    private OrderDetailService orderDetailService;

    @RequestMapping("confirm")
    public RetVal confirm(HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        //1. userAddressList  用户的收货地址
        List<UserAddress> userAddressList = userFeignClient.getUserAddressByUserId(userId);
        // 订单的送货清单
        List<CartInfo> selectedCartInfo = cartFeignClient.getSelectedCartInfo(userId);

        //3. detailArrayList 用上述材料加价格 数量 生成
        List<OrderDetail> orderDetailList = new ArrayList<>();
        int totalNum = 0;
        BigDecimal totalPrice = new BigDecimal(0);
        if (!CollectionUtils.isEmpty(selectedCartInfo)) {
            for (CartInfo cartInfo : selectedCartInfo) {
                OrderDetail orderDetail = new OrderDetail();
                //detail.imgUrl skuName orderPrice skuNum
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuNum(cartInfo.getSkuNum() + "");
                //orderPrice
                orderDetail.setOrderPrice(cartInfo.getCartPrice());
                totalPrice = totalPrice.add(cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum())));
                //总数量
                totalNum += cartInfo.getSkuNum();
                //orderDetail 加入集合
                orderDetailList.add(orderDetail);
            }
        }
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("userAddressList", userAddressList);
        retMap.put("detailArrayList", orderDetailList);
        retMap.put("totalNum", totalNum);
        retMap.put("totalMoney", totalPrice);

        //4. 生成一个流水号
        String tradeNo = orderInfoService.generateTradeNo(userId);
        retMap.put("tradeNo", tradeNo);

        return RetVal.ok(retMap);
    }

    // 提交订单  http://api.gmall.com/order/submitOrder?tradeNo=xxxx
    @PostMapping("submitOrder")
    public RetVal submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        String tradeNoUI = request.getParameter("tradeNo");

        // tradeNo同redis里面的tradeNo进行比对
        boolean flag = orderInfoService.compareTradeNo(userId, tradeNoUI);
        if (!flag) {
            return RetVal.fail().message("不能重复提交订单信息");
        }
        //验证商品库存和价格
        String warnMsg = orderInfoService.checkStockAndPrice(userId, orderInfo);
        // 保存订单基本信息和详细信息
        Long orderId = orderInfoService.saveOrderAndDetail(orderInfo);
        // 保存完后删除流水号
        orderInfoService.deleteTradeNo(userId);

        return RetVal.ok(orderId);


    }

    @GetMapping("/getOrderInfoByOrderId/{orderId}")
    public OrderInfo getOrderInfoByOrderId(@PathVariable Long orderId) {
        //1. 获得订单基本信息
        return orderInfoService.getOrderInfo(orderId);
    }

    //拆单接口
    @PostMapping("splitOrder")
    public String splitOrder(@RequestParam Long orderId, @RequestParam String wareHouseIdSkuIdMapJson) {
        return orderInfoService.splitOrder(orderId, wareHouseIdSkuIdMapJson);
    }

    // 保存订单及详情
    @PostMapping("/saveOrderAndDetail")
    public Long saveOrderAndDetail(@RequestBody OrderInfo orderInfo) {
        return orderInfoService.saveOrderAndDetail(orderInfo);
    }
}

