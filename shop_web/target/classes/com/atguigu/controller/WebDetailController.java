package com.atguigu.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.client.ProductFeignClient;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/7/26 11:05 周二
 * description:
 */
@Controller
public class WebDetailController {
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private ThreadPoolExecutor myPoolExecutor;

    @RequestMapping("{skuId}.html")
    public String getSkuDetail(@PathVariable Long skuId, Model model) {
        //a.根据skuId查询商品的基本信息
        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            model.addAttribute("skuInfo", skuInfo);
            return skuInfo;
        }, myPoolExecutor);

        //b.根据三级分类id查询商品的分类
        CompletableFuture<Void> categoryViewFuture = skuInfoCompletableFuture.thenAcceptAsync((SkuInfo skuInfo) -> {
            Long category3Id = skuInfo.getCategory3Id();
            BaseCategoryView categoryView = productFeignClient.getCategoryView(category3Id);
            model.addAttribute("categoryView", categoryView);
        }, myPoolExecutor);

        //c.根据skuId查询商品的实时价格
        CompletableFuture<Void> skuPriceFuture = CompletableFuture.runAsync(() -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            model.addAttribute("price", skuPrice);
        }, myPoolExecutor);

        //d.销售属性id的组合与skuId的对应关系
        CompletableFuture<Void> salePropertyAndSkuIdMappingFuture = skuInfoCompletableFuture.thenAcceptAsync((SkuInfo skuInfo) -> {
            Long productId = skuInfo.getProductId();
            Map<Object, Object> salePropertyAndSkuIdMapping = productFeignClient.getSalePropertyAndSkuIdMapping(productId);
            model.addAttribute("salePropertyValueIdJson", JSON.toJSONString(salePropertyAndSkuIdMapping));
        }, myPoolExecutor);

        //e.获取所有的销售属性(spu全份)和sku的销售属性(一份)
        CompletableFuture<Void> spuSalePropertyListFuture = skuInfoCompletableFuture.thenAcceptAsync((SkuInfo skuInfo) -> {
            Long productId = skuInfo.getProductId();
            List<ProductSalePropertyKey> spuSalePropertyList = productFeignClient.getSpuSalePropertyAndSelected(productId, skuId);
            model.addAttribute("spuSalePropertyList", spuSalePropertyList);
        }, myPoolExecutor);
        CompletableFuture.allOf(skuInfoCompletableFuture, categoryViewFuture, skuPriceFuture, salePropertyAndSkuIdMappingFuture, spuSalePropertyListFuture).join();
        return "detail/index";

    }

}
