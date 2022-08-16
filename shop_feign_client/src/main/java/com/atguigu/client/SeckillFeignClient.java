package com.atguigu.client;

import com.atguigu.entity.SeckillProduct;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@FeignClient(value = "shop-seckill")
public interface SeckillFeignClient {

    @GetMapping("/seckill/queryAllSecKillProduct")
    public List<SeckillProduct> queryAllSecKillProduct();

    @GetMapping("/seckill/querySecKillProductById/{skuId}")
    public SeckillProduct querySecKillProductById(@PathVariable Long skuId);
}
