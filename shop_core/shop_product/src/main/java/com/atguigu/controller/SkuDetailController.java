package com.atguigu.controller;

import com.atguigu.cache.ShopCache;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import com.atguigu.service.BaseCategoryViewService;
import com.atguigu.service.SkuDetailService;
import com.atguigu.service.SkuInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/7/25 21:37 周一
 * description:
 */
@RestController
@RequestMapping("/sku")
public class SkuDetailController {

    @Autowired
    private SkuInfoService skuInfoService;
    @Autowired
    private BaseCategoryViewService categoryViewService;
    @Autowired
    private SkuDetailService skuDetailService;

    //a. 根据skuId查询商品的基本信息
    @GetMapping("getSkuInfo/{skuId}")
    @ShopCache(prefix = "getSkuInfo:")
    public SkuInfo getSkuInfo(@PathVariable Long skuId) {
        SkuInfo skuInfo = skuDetailService.getInfoById(skuId);
        return skuInfo;
    }
@Transactional
    //b. 根据三级分类查询商品的分类
    @GetMapping("getCategoryView/{category3Id}")
    @ShopCache(prefix = "getCategoryView:")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id) {
        return categoryViewService.getById(category3Id);
    }

    //c. 根据skuId查询商品的实时价格
    @GetMapping("getSkuPrice/{skuId}")
    @ShopCache(prefix = "getSkuPrice:")
    public BigDecimal getSkuPrice(@PathVariable Long skuId) {
        SkuInfo skuInfo = skuInfoService.getById(skuId);
        return skuInfo.getPrice();
    }

    //d. 销售属性id的组合与skuId的对应关系 根据productId, skuId查询商品销售属性key与value
    @GetMapping("getSalePropertyAndSkuIdMapping/{productId}")
    @ShopCache(prefix = "getSalePropertyAndSkuIdMapping:")
    public Map<Object, Object> getSalePropertyAndSkuIdMapping(@PathVariable Long productId) {
        return skuDetailService.getSalePropertyAndSkuIdMapping(productId);

    }

    //e. 获取所有的销售属性(spu全份)和sku的销售属性(一份)  通过spuId获取对应的字符串
    @GetMapping("getSpuSalePropertyAndSelected/{productId}/{skuId}")
   // @ShopCache(prefix = "getSpuSalePropertyAndSelected:")
    public List<ProductSalePropertyKey> getSpuSalePropertyAndSelected(@PathVariable Long productId, @PathVariable Long skuId) {
        return skuDetailService.getSpuSalePropertyAndSelected(productId, skuId);
    }
}
