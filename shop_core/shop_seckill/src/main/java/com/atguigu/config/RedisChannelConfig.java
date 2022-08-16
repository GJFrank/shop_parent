package com.atguigu.config;

import com.atguigu.constant.RedisConst;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/8/15 12:51 周一
 * description:
 */
@Configuration
public class RedisChannelConfig {
    //定义发布订阅 当有消息的时候接收处理消息的方法
    @Bean
    MessageListenerAdapter listenerAdapter(ShopMessageReceiver receiver) {
        return new MessageListenerAdapter(receiver, "receiveChannelMessage");
    }

    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        //订阅哪个主题
        container.addMessageListener(listenerAdapter, new PatternTopic(RedisConst.PREPARE_PUB_SUB_SECKILL));
        return container;
    }
}

