package com.atguigu.client;

import com.atguigu.entity.BaseCategoryView;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import com.atguigu.result.RetVal;
import com.atguigu.search.SearchParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

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
    @FeignClient(value = "shop-search")
public interface SearchFeignClient {
    //a. 商品的上架
    @GetMapping("/search/onSale/{skuId}")
    public RetVal onSale(@PathVariable Long skuId) ;

    //b. 商品的下架
    @GetMapping("/search/offSale/{skuId}")
    public RetVal offSale(@PathVariable Long skuId) ;


//商品的搜索
    @PostMapping("/search/searchProduct")
    public RetVal searchProduct(SearchParam searchParam) ;
}
