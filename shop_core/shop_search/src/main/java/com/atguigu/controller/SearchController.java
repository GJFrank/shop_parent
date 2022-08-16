package com.atguigu.controller;

import com.atguigu.result.RetVal;
import com.atguigu.search.Product;
import com.atguigu.search.SearchParam;
import com.atguigu.search.SearchResponseVo;
import com.atguigu.service.SearchService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/8/3 13:23 周三
 * description:
 */
@RestController
@RequestMapping("/search")
public class SearchController {
    @Autowired
    private ElasticsearchRestTemplate esTemplate;

    @Autowired
    private SearchService searchService;

    //1. 创建索引
    @GetMapping("/createIndex")
    public RetVal createIndex() {
        esTemplate.createIndex(Product.class);
        esTemplate.putMapping(Product.class);
        return RetVal.ok();
    }

    //2. 商品的上架, 产品从数据库到es
    @GetMapping("/onSale/{skuId}")
    public RetVal onSale(@PathVariable Long skuId) {
        searchService.onSale(skuId);
        return RetVal.ok();
    }

    //3. 商品的下架, 产品从es删除
    @GetMapping("/offSale/{skuId}")
    public RetVal offSale(@PathVariable Long skuId) {
        searchService.offSale(skuId);
        return RetVal.ok();
    }


    //4. 商品的搜索
    @PostMapping("/searchProduct")
    public RetVal searchProduct(@RequestBody SearchParam searchParam) {
        SearchResponseVo searchResponseVo = searchService.searchProduct(searchParam);
        return RetVal.ok(searchResponseVo);
    }
}
