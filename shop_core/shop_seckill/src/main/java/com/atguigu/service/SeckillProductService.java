package com.atguigu.service;

import com.atguigu.entity.SeckillProduct;
import com.atguigu.entity.UserSeckillSkuInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author GodWei
 * @since 2022-08-15
 */
public interface SeckillProductService extends IService<SeckillProduct> {

    SeckillProduct getSeckillProductById(Long skuId);

    void prepareSecKill(UserSeckillSkuInfo userSeckillSkuInfo);

}
