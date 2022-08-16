package com.atguigu.client;

import com.atguigu.entity.*;
import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/7/25 20:47 周一
 * description:
 */
//指明接口由哪个微服务去实现
@FeignClient(value = "shop-product")
public interface ProductFeignClient {
    //a. 根据skuId查询商品的基本信息
    @GetMapping("/sku/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId);

    //b. 根据三级分类查询商品的分类
    @GetMapping("/sku/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id);

    //c. 根据skuId查询商品的实时价格
    @GetMapping("/sku/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId);

    //d. 销售属性id的组合与skuId的对应关系 根据productId, skuId查询商品销售属性key与value
    @GetMapping("/sku/getSalePropertyAndSkuIdMapping/{productId}")
    public Map<Object, Object> getSalePropertyAndSkuIdMapping(@PathVariable Long productId);

    //e. 获取所有的销售属性(spu全份)和sku的销售属性(一份)  通过spuId获取对应的字符串
    @GetMapping("/sku/getSpuSalePropertyAndSelected/{productId}/{skuId}")
    public List<ProductSalePropertyKey> getSpuSalePropertyAndSelected(@PathVariable Long productId, @PathVariable Long skuId);

    //f. 首页获取所有分类信息
    @GetMapping("/product/getIndexCategory")
    public RetVal getIndexCategory();

    // 获取品牌信息
    @GetMapping("/product/brand/getBrandById/{brandId}")
    public BaseBrand getBrandById(@PathVariable Long brandId);

    // 获取品台属性  根据skuId
    // List<PlatformPropertyKey> platformPropertyKeyList=productFeignClient.getPlatformBySkuId(skuId);
    @GetMapping("/product/getPlatformBySkuId/{skuId}")
    public List<PlatformPropertyKey> getPlatformBySkuId(@PathVariable Long skuId);













































}
