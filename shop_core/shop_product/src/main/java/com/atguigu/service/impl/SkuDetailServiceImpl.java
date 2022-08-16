package com.atguigu.service.impl;

import com.atguigu.constant.RedisConst;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuImage;
import com.atguigu.entity.SkuInfo;
import com.atguigu.mapper.ProductSalePropertyKeyMapper;
import com.atguigu.mapper.SkuInfoMapper;
import com.atguigu.service.SkuDetailService;
import com.atguigu.service.SkuImageService;
import com.atguigu.service.SkuInfoService;
import com.atguigu.service.SkuSalePropertyValueService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/7/26 9:11 周二
 * description:
 */
@Service
public class SkuDetailServiceImpl implements SkuDetailService {
    @Autowired
    private SkuInfoService skuInfoService;
    @Autowired
    private SkuImageService skuImageService;
    @Autowired
    private SkuSalePropertyValueService salePropertyValueService;
    @Autowired
    private ProductSalePropertyKeyMapper salePropertyKeyMapper;
    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Override
    public SkuInfo getInfoById(Long skuId) {
        return skuInfoMapper.selectById(skuId);
    }

    @Override
    public Map<Object, Object> getSalePropertyAndSkuIdMapping(Long productId) {
        Map<Object, Object> salePropertyAndSkuIdMap = new HashMap<>();
        List<Map> retListMap = salePropertyValueService.getSalePropertyAndSkuIdMapping(productId);
        for (Map map : retListMap) {
            salePropertyAndSkuIdMap.put(map.get("sale_property_value_id"), map.get("sku_id"));
        }
        return salePropertyAndSkuIdMap;
    }


    //e. 获取所有的销售属性(spu全份)和sku的销售属性(一份)  通过spuId获取对应的字符串
    @Override
    public List<ProductSalePropertyKey> getSpuSalePropertyAndSelected(Long productId, Long skuId) {
        return salePropertyKeyMapper.getSpuSalePropertyAndSelected(productId, skuId);
    }


}
