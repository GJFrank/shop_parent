package com.atguigu.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/7/29 9:30 周五
 * description:
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
public class RedissonConfig {
    //host password port timeout address_prefix
    private String password;
    private String host;
    private String port;
    private int timeout = 3000;
    private static String ADDRESS_PREFIX = "redis://";

    /*自动装配*/
    @Bean
    RedissonClient redissonClient() {
        Config config = new Config();
        if (StringUtils.isEmpty(host)) {
            throw new RuntimeException("host is empty");
        }
        SingleServerConfig serverConfig = config.useSingleServer().setAddress(ADDRESS_PREFIX + this.host + ":" + this.port).setTimeout(this.timeout);
        if (!StringUtils.isEmpty(this.password)) {
            serverConfig.setPassword(this.password);
        }
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }

}
