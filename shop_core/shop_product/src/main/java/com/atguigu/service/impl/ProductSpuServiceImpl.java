package com.atguigu.service.impl;

import com.atguigu.entity.ProductImage;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.ProductSalePropertyValue;
import com.atguigu.entity.ProductSpu;
import com.atguigu.mapper.ProductSpuMapper;
import com.atguigu.service.ProductImageService;
import com.atguigu.service.ProductSalePropertyKeyService;
import com.atguigu.service.ProductSalePropertyValueService;
import com.atguigu.service.ProductSpuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author GodWei
 * @since 2022-07-24
 */
@Service
public class ProductSpuServiceImpl extends ServiceImpl<ProductSpuMapper, ProductSpu> implements ProductSpuService {


    @Autowired
    private ProductImageService productImageService;
    @Autowired
    private ProductSalePropertyValueService salePropertyValueService;
    @Autowired
    private ProductSalePropertyKeyService salePropertyKeyService;

    @Transactional
    @Override
    public void saveProductSpu(ProductSpu productSpu) {
        //保存productSpu信息
        baseMapper.insert(productSpu);


        //批量保存productImage信息
        List<ProductImage> productImageList = productSpu.getProductImageList();
        //若非空
        if (!CollectionUtils.isEmpty(productImageList)) {
            for (ProductImage productImage : productImageList) {
                productImage.setProductId(productSpu.getId());
            }
            productImageService.saveBatch(productImageList);
        }

        //批量保存销售属性key和value信息
        List<ProductSalePropertyKey> salePropertyKeyList = productSpu.getSalePropertyKeyList();
        if (!CollectionUtils.isEmpty(salePropertyKeyList)) {
            for (ProductSalePropertyKey productSalePropertyKey : salePropertyKeyList) {
                productSalePropertyKey.setProductId(productSpu.getId());

                //获取里面的valuelist
                List<ProductSalePropertyValue> valueList = productSalePropertyKey.getSalePropertyValueList();
                if (!CollectionUtils.isEmpty(valueList)) {
                    for (ProductSalePropertyValue salePropertyValue : valueList) {
                        salePropertyValue.setProductId(productSpu.getId());
                        salePropertyValue.setSalePropertyKeyName(productSalePropertyKey.getSalePropertyKeyName());
                        //salePropertyValue.setSalePropertyValueName(salePropertyValue.getSalePropertyValueName());
                    }
                    salePropertyValueService.saveBatch(valueList);
                }
            }
            salePropertyKeyService.saveBatch(salePropertyKeyList);
        }
    }
}
