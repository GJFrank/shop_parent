package com.atguigu.executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/8/1 21:39 周一
 * description:
 */
@Configuration
@EnableConfigurationProperties(MyThreadPoolProperties.class)
public class MyThreadPoolConfig {
    @Autowired
    private MyThreadPoolProperties myThreadPoolProperties;

    @Bean
    public ThreadPoolExecutor myExecutors() {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                myThreadPoolProperties.getCorePoolSize(),
                myThreadPoolProperties.getMaximumPoolSize(),
                myThreadPoolProperties.getKeepAliveTime(),
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(myThreadPoolProperties.getQueueLength()),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
        return threadPoolExecutor;
    }
}
