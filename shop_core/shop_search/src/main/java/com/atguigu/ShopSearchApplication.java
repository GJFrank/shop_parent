package com.atguigu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.Async;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/8/2 21:07 周二
 * description:
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableFeignClients
@EnableDiscoveryClient
//@Async
public class ShopSearchApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShopSearchApplication.class, args);
    }
}
