package com.atguigu.service;

import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;

import java.util.List;
import java.util.Map;

public interface SkuDetailService {
    List<ProductSalePropertyKey> getSpuSalePropertyAndSelected(Long productId,Long skuId);
    SkuInfo getInfoById(Long skuId);

    Map<Object, Object> getSalePropertyAndSkuIdMapping(Long productId);
}
