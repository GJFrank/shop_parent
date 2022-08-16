package com.atguigu.controller;


import com.atguigu.entity.PlatformPropertyKey;
import com.atguigu.entity.PlatformPropertyValue;
import com.atguigu.result.RetVal;
import com.atguigu.service.PlatformPropertyKeyService;
import com.atguigu.service.PlatformPropertyValueService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.PublicKey;
import java.util.List;

/**
 * <p>
 * 属性表 前端控制器
 * </p>
 *
 * @author GodWei
 * @since 2022-07-20
 * //getPlatformPropertyByCategoryId/1/0/0
 */

@RestController
@RequestMapping("/product")
public class PlatformPropertyController {

    @Autowired
    private PlatformPropertyKeyService propertyKeyService;
    @Autowired
    private PlatformPropertyValueService propertyValueService;

    //http://127.0.0.1/product/getPropertyValueByPropertyKeyId/4
    @GetMapping("getPropertyValueByPropertyKeyId/{propertyKeyId}")
    public RetVal getPropertyValueByPropertyKeyId(@PathVariable Long propertyKeyId) {
        QueryWrapper<PlatformPropertyValue> platformPropertyValueQueryWrapper = new QueryWrapper<>();
        platformPropertyValueQueryWrapper.eq("property_key_id", propertyKeyId);
        List<PlatformPropertyValue> list = propertyValueService.list(platformPropertyValueQueryWrapper);
        return RetVal.ok(list);
    }

    //保存平台属性  若已存在,则进行先删除后insert操作 在serviceimpl
    // http://127.0.0.1/product/savePlatformProperty
    @PostMapping("savePlatformProperty")   //@RequestBody 将json字符串转换成对象
    public RetVal savePlatformProperty(@RequestBody PlatformPropertyKey platformProperty) {
        boolean flag = propertyKeyService.savePlatformProperty(platformProperty);
        if (flag) {
            return RetVal.ok();
        } else {
            return RetVal.fail();
        }
    }

    @GetMapping("getPlatformPropertyByCategoryId/{category1Id}/{category2Id}/{category3Id}")
    public RetVal getPlatformPropertyByCategoryId(
            @PathVariable Long category1Id,
            @PathVariable Long category2Id,
            @PathVariable Long category3Id
    ) {
        List<PlatformPropertyKey> platformPropertyKeyList = propertyKeyService.getPlatformPropertyByCategoryId(category1Id, category2Id, category3Id);
        return RetVal.ok(platformPropertyKeyList);
    }

    @GetMapping("getPlatformBySkuId/{skuId}")
    public List<PlatformPropertyKey> getPlatformBySkuId(@PathVariable Long skuId){
        return  propertyKeyService.getPlatformBySkuId(skuId);
    }





















}

