package com.atguigu.controller;

import com.atguigu.client.CartFeignClient;
import com.atguigu.client.OrderFeignClient;
import com.atguigu.client.ProductFeignClient;
import com.atguigu.entity.OrderInfo;
import com.atguigu.entity.SkuInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/8/8 11:37 周一
 * description:
 */
@Controller
public class WebPaymentController {
    @Autowired
    private OrderFeignClient orderFeignClient;

    //1. 跳转到支付页面
    @RequestMapping("pay.html")
    public String pay(Long orderId, Model model) {
        //1. 获得订单
        OrderInfo orderInfo = orderFeignClient.getOrderInfoByOrderId(orderId);
        //2.根据前端页面把orderInfo对象放入请求域
        model.addAttribute("orderInfo", orderInfo);
        return "payment/pay";
    }

    //    //2. 支付成功之后的跳转页面
//    @RequestMapping("alipay/success.html")
//    public String success() {
//        return "payment/success";
//    }
//2. 支付成功之后的跳转页面  同步通知 shop-web
    @RequestMapping("alipay/success.html")
    public String success() {
        return "payment/success";
    }

}
