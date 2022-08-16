package com.atguigu.controller;

import com.atguigu.constant.MqConst;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/8/15 9:49 周一
 * description:
 */
@RestController
@RequestMapping
public class SimulateQuartzController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    //1. 发送一个上架秒杀商品的消息
    @GetMapping("sendMsgToScanSecKill")
    public String sendMsgToScanSecKill() {
        //发送消息只是起到一个通知的效果
        rabbitTemplate.convertAndSend(MqConst.SCAN_SECKILL_EXCHANGE, MqConst.SCAN_SECKILL_ROUTE_KEY, "niXX");
        return "success";
    }

}
