package com.atguigu.config;

import com.atguigu.constant.MqConst;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/8/10 21:00 周三
 * description:
 */
@Configuration
public class DelayedQueueConfig {

    //队列
    @Bean
    public Queue cancelOrderQueue() {
        return new Queue(MqConst.CANCEL_ORDER_QUEUE, false);
    }

    //交换机
    @Bean
    public CustomExchange cancelOrderExchange() {
        Map<String, Object> params = new HashMap<>();
        params.put("x-delayed-type", "direct");
        return new CustomExchange(MqConst.CANCEL_ORDER_EXCHANGE, "x-delayed-message", false, false, params);
    }

    //绑定关系
    @Bean
    public Binding bindingDelayedQueue(@Qualifier("cancelOrderQueue") Queue cancelOrderQueue,
                                       @Qualifier("cancelOrderExchange") CustomExchange cancelOrderExchange) {
        return BindingBuilder.bind(cancelOrderQueue).to(cancelOrderExchange).with(MqConst.CANCEL_ORDER_ROUTE_KEY).noargs();

    }

}
