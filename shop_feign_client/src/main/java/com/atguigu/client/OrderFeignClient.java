package com.atguigu.client;

import com.atguigu.entity.CartInfo;
import com.atguigu.entity.OrderInfo;
import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/7/25 20:47 周一
 * description:
 */
//指明接口由哪个微服务去实现
@FeignClient(value = "shop-order")
public interface OrderFeignClient {
    @RequestMapping("/order/confirm")
    public RetVal confirm();

    @GetMapping("/order/getOrderInfoByOrderId/{orderId}")
    public OrderInfo getOrderInfoByOrderId(@PathVariable Long orderId);

    // 保存订单及详情
    @PostMapping("/order/saveOrderAndDetail")
   public Long saveOrderAndDetail(@RequestBody OrderInfo orderInfo);
}
