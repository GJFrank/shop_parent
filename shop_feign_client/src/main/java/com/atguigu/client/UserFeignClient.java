package com.atguigu.client;

import com.atguigu.entity.UserAddress;
import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/7/25 20:47 周一
 * description:
 */
//指明接口由哪个微服务去实现
@FeignClient(value = "shop-user")
public interface UserFeignClient {
    //根据用户id查找用户地址
    @GetMapping("/user/getUserAddressByUserId/{userId}")
    public List<UserAddress> getUserAddressByUserId(@PathVariable String userId);

}
