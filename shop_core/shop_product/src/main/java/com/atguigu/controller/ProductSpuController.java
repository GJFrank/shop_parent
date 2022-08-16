package com.atguigu.controller;


import com.atguigu.entity.BaseSaleProperty;
import com.atguigu.entity.ProductSpu;
import com.atguigu.result.RetVal;
import com.atguigu.service.BaseSalePropertyService;
import com.atguigu.service.ProductSalePropertyKeyService;
import com.atguigu.service.ProductSpuService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 商品表 前端控制器
 * </p>
 *
 * @author GodWei
 * @since 2022-07-24
 */
@RestController
@RequestMapping("/product")
public class ProductSpuController {
    @Autowired
    private ProductSpuService productSpuService;
    @Autowired
    private BaseSalePropertyService salePropertyService;

    //根据分类id查询商品spu列表
    @GetMapping("queryProductSpuByPage/{pageNum}/{pageSize}/{category3Id}")
    public RetVal queryProductSpuByPage(@PathVariable Long pageNum, @PathVariable Long pageSize, @PathVariable Long category3Id) {
        Page<ProductSpu> productSpuPage = new Page<>(pageNum, pageSize);
        QueryWrapper<ProductSpu> wrapper = new QueryWrapper<>();
        wrapper.eq("category3_id", category3Id);
        productSpuService.page(productSpuPage, wrapper);
        return RetVal.ok(productSpuPage);
    }

    //查询所有的销售属性
    @GetMapping("queryAllSaleProperty")
    public RetVal queryAllSaleProperty() {
        List<BaseSaleProperty> salePropertyList = salePropertyService.list(null);
        return RetVal.ok(salePropertyList);
    }

    //保存商品spu信息
    @PostMapping("saveProductSpu")
    public RetVal saveProductSpu(@RequestBody ProductSpu productSpu) {
        productSpuService.saveProductSpu(productSpu);
        return RetVal.ok();
    }

}

