package com.atguigu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/8/12 18:19 周五
 * description:
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class PaymentApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }
}
