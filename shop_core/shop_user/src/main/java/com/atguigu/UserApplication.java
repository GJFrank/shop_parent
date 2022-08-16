package com.atguigu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/8/6 16:21 周六
 * description:
 */
@SpringBootApplication
@EnableDiscoveryClient  //开启服务注册发现功能
public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
