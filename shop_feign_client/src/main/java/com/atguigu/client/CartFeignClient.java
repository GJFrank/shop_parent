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
@FeignClient(value = "shop-cart")
public interface CartFeignClient {
    //a. 加入购物车
    @GetMapping("/cart/addCart/{skuId}/{skuNum}")
    public RetVal addCart(@PathVariable Long skuId, @PathVariable Long skuNum);

    //b. 订单的送货清单
    @GetMapping("/cart/getSelectedCartInfo/{userId}")
    public List<CartInfo> getSelectedCartInfo(@PathVariable String userId);
}
