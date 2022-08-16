package com.atguigu.controller;

import com.atguigu.client.SearchFeignClient;
import com.atguigu.constant.MqConst;
import com.atguigu.entity.ProductImage;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import com.atguigu.result.RetVal;
import com.atguigu.service.ProductImageService;
import com.atguigu.service.ProductSalePropertyKeyService;
import com.atguigu.service.SkuInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/7/24 21:38 周日
 * description:
 */
@RestController
@RequestMapping("/product")
public class ProductSkuController {

    @Autowired
    private SkuInfoService skuInfoService;
    @Autowired
    private ProductSalePropertyKeyService salePropertyKeyService;
    @Autowired
    private ProductImageService productImageService;
    @Autowired
    private SearchFeignClient searchFeignClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    //商品下架 http://api.gmall.com/product/offSale/37
    @GetMapping("offSale/{skuId}")
    public RetVal offSale(@PathVariable Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(0);
        skuInfoService.updateById(skuInfo);

        //商品从es删除
        // searchFeignClient.offSale(skuId);
        rabbitTemplate.convertAndSend(MqConst.ON_OFF_SALE_EXCHANGE,MqConst.OFF_SALE_ROUTING_KEY,skuId);
        return RetVal.ok();
    }

    //商品上架 http://api.gmall.com/product/onSale/38
    @GetMapping("onSale/{skuId}")
    public RetVal onSale(@PathVariable Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(1);
        skuInfoService.updateById(skuInfo);

        //商品中加入es中  todo 使用消息队列
        //searchFeignClient.onSale(skuId);
        rabbitTemplate.convertAndSend(MqConst.ON_OFF_SALE_EXCHANGE,MqConst.ON_SALE_ROUTING_KEY,skuId);

        return RetVal.ok();
    }

    //
    @RequestMapping("querySkuInfoByPage/{currentNum}/{pageSize}")
    public RetVal querySkuInfoByPage(@PathVariable Long currentNum, @PathVariable Long pageSize) {
        Page<SkuInfo> skuInfoPage = new Page<>(currentNum, pageSize);
        QueryWrapper<SkuInfo> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("id");
        skuInfoService.page(skuInfoPage, wrapper);
        return RetVal.ok(skuInfoPage);
    }

    //根据spuId查找所有的销售属性
    // GET http://127.0.0.1/product/querySalePropertyByProductId/15
    @GetMapping("querySalePropertyByProductId/{productId}")
    public RetVal querySalePropertyByProductId(@PathVariable Long productId) {
        List<ProductSalePropertyKey> salePropertyKeyList = salePropertyKeyService.querySalePropertyByProductId(productId);
        return RetVal.ok(salePropertyKeyList);
    }

    //需要根据spuId 查找所有的spu图片
    //GET http://127.0.0.1/product/queryProductImageByProductId/15 404 (Not Found)
    @GetMapping("queryProductImageByProductId/{productId}")
    public RetVal queryProductImageByProductId(@PathVariable Long productId) {
        QueryWrapper<ProductImage> productImageQueryWrapper = new QueryWrapper<>();
        productImageQueryWrapper.eq("product_id", productId);
        List<ProductImage> list = productImageService.list(productImageQueryWrapper);
        return RetVal.ok(list);
    }

    //保存单个sku信息  POST http://127.0.0.1/product/saveSkuInfo 404 (Not Found)
    @PostMapping("saveSkuInfo")
    public RetVal saveSkuInfo(@RequestBody SkuInfo skuInfo) {
        skuInfoService.saveSkuInfo(skuInfo);
        return RetVal.ok();
    }

}
