package com.atguigu.controller;


import com.atguigu.entity.UserAddress;
import com.atguigu.result.RetVal;
import com.atguigu.service.UserAddressService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 用户地址表 前端控制器
 * </p>
 *
 * @author GodWei
 * @since 2022-08-06
 */
@RestController
@RequestMapping("/user")
public class UserAddressController {

    @Autowired
    private UserAddressService userAddressService;

    @GetMapping("getUserAddressByUserId/{userId}")
    public List<UserAddress> getUserAddressByUserId(@PathVariable String userId) {
        QueryWrapper<UserAddress> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        return userAddressService.list(wrapper);
    }
}

