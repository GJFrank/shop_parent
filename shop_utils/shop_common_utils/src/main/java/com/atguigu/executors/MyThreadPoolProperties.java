package com.atguigu.executors;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/8/1 21:40 周一
 * description:
 */
@Data
@ConfigurationProperties(prefix = "thread.pool")
public class MyThreadPoolProperties {
    public Integer corePoolSize = 16;  //核心线程数
    public Integer maximumPoolSize = 32; //最大线程数
    public Long keepAliveTime = 50L; //空闲线程的存活时间
    public Integer queueLength = 100; //用于缓存任务的阻塞队列
}

