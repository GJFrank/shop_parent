package com.atguigu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/7/20 18:28 周三
 * description:
 */
@EnableDiscoveryClient
@SpringBootApplication
@EnableFeignClients

public class ProductApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductApplication.class, args);
    }
}
