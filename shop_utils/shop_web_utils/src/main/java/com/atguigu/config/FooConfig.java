package com.atguigu.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/8/1 20:29 周一
 * description:
 */
@Configuration
public class FooConfig {
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
