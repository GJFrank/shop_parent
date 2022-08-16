package com.atguigu.service.impl;

import com.atguigu.entity.SkuImage;
import com.atguigu.entity.SkuInfo;
import com.atguigu.entity.SkuPlatformPropertyValue;
import com.atguigu.entity.SkuSalePropertyValue;
import com.atguigu.mapper.SkuInfoMapper;
import com.atguigu.service.SkuImageService;
import com.atguigu.service.SkuInfoService;
import com.atguigu.service.SkuPlatformPropertyValueService;
import com.atguigu.service.SkuSalePropertyValueService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * <p>
 * 库存单元表 服务实现类
 * </p>
 *
 * @author GodWei
 * @since 2022-07-24
 */
@Service
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoMapper, SkuInfo> implements SkuInfoService {
    @Autowired
    private SkuImageService skuImageService;
    @Autowired
    private SkuPlatformPropertyValueService skuPlatformPropertyValueService;
    @Autowired
    private SkuSalePropertyValueService skuSalePropertyValueService;

    @Transactional
    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        //1. 插入skuinfo
        baseMapper.insert(skuInfo);
        //1.2 skuInfo中的imageList
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)) {
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
            }
            skuImageService.saveBatch(skuImageList);
        }
        //1.3 sku平台属性值
        List<SkuPlatformPropertyValue> skuPlatformPropertyValueList = skuInfo.getSkuPlatformPropertyValueList();
        if (!CollectionUtils.isEmpty(skuPlatformPropertyValueList)) {
            for (SkuPlatformPropertyValue skuPlatformPropertyValue : skuPlatformPropertyValueList) {
                skuPlatformPropertyValue.setSkuId(skuInfo.getId());
            }
            skuPlatformPropertyValueService.saveBatch(skuPlatformPropertyValueList);

        }
        //1.4 sku销售属性值
        List<SkuSalePropertyValue> skuSalePropertyValueList = skuInfo.getSkuSalePropertyValueList();
        if (!CollectionUtils.isEmpty(skuSalePropertyValueList)) {
            for (SkuSalePropertyValue skuSalePropertyValue : skuSalePropertyValueList) {
                skuSalePropertyValue.setSkuId(skuInfo.getId());
                skuSalePropertyValue.setProductId(skuInfo.getProductId());
            }
            skuSalePropertyValueService.saveBatch(skuSalePropertyValueList);
        }
    }
}
