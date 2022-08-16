package com.atguigu.config;

import com.atguigu.constant.RedisConst;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/8/1 8:33 周一
 * description:
 */
@Configuration
public class BloomFilterConfig {
    @Autowired
    private RedissonClient redissonClient;

    @Bean
    public RBloomFilter<Long> skuBloomFilter() {

        //指定过滤器名称
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConst.BLOOM_SKU_ID);
        //初始化布隆过滤器容器大小, 期望的存储精度是多少
        bloomFilter.tryInit(10000, 0.001);
        return bloomFilter;
    }
}
