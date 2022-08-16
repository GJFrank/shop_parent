package com.atguigu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/8/15 9:26 周一
 * description:
 */
@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
public class SeckillApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
    }
}
