package com.atguigu.consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.constant.MqConst;
import com.atguigu.entity.OrderInfo;
import com.atguigu.enums.OrderStatus;
import com.atguigu.enums.ProcessStatus;
import com.atguigu.service.OrderInfoService;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/8/10 21:09 周三
 * description: 本来应该集中在一个微服务里写的, 这里为了方便
 */
@Component
public class OrderConsumer {
    @Autowired
    private OrderInfoService orderInfoService;

    // 超时未支付 自动取消订单
    @RabbitListener(queues = MqConst.CANCEL_ORDER_QUEUE)
    public void cancelOrder(Long orderId) {
        OrderInfo orderInfo = orderInfoService.getById(orderId);
        orderInfo.setOrderStatus(OrderStatus.CLOSED.name());
        orderInfo.setProcessStatus(ProcessStatus.CLOSED.name());

        orderInfoService.updateById(orderInfo);
        //Todo Order还有后续事情
    }

    //2.支付成功之后修改订单状态   --没做配置类, 直接使用注解
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.PAY_ORDER_QUEUE, durable = "false"),
            exchange = @Exchange(value = MqConst.PAY_ORDER_EXCHANGE, durable = "false"),
            key = {MqConst.PAY_ORDER_ROUTE_KEY}
    ))
    public void updateOrderAfterPaySuccess(Long orderId) {
        if (orderId != null) {
            // 查询订单基本信息和详细信息 是 为了接下来减库存使用
            OrderInfo orderInfo = orderInfoService.getOrderInfo(orderId);
            //当订单状态为未支付的时候才能使用

            if (orderInfo != null && orderInfo.getProcessStatus().equals(ProcessStatus.UNPAID.name())) {
                //更新订单状态,和进度状态
                orderInfoService.updateOrderStatusByProcessStatus(orderInfo, ProcessStatus.PAID);
                //通知仓库系统减库存
                orderInfoService.sendMessageToWareHouse(orderInfo);
            }
        }
    }

    //3. 仓库系统减库存之后的代码
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.SUCCESS_DECREASE_STOCK_QUEUE, durable = "false"),
            exchange = @Exchange(value = MqConst.SUCCESS_DECREASE_STOCK_EXCHANGE, durable = "false"),
            key = {MqConst.SUCCESS_DECREASE_STOCK_ROUTE_KEY}
    ))
    public void updateOrderAfterDecrease(String jsonString) {
        if (!StringUtils.isEmpty(jsonString)) {
            Map<String, Object> map = JSON.parseObject(jsonString);
            String orderId = (String) map.get("orderId");
            String status = (String) map.get("status");

            //如果仓库系统减库存成功, 把订单状态改为等待发货
            OrderInfo orderInfo = orderInfoService.getOrderInfo(Long.parseLong(orderId));
            if ("DEDUCTED".equals(status)) {
                orderInfoService.updateOrderStatusByProcessStatus(orderInfo, ProcessStatus.WAITING_DELEVER);
            } else {
                orderInfoService.updateOrderStatusByProcessStatus(orderInfo, ProcessStatus.STOCK_EXCEPTION);
            }
        }
    }
}
