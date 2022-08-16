package com.atguigu.controller;


import com.alibaba.fastjson.JSONObject;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.UserInfo;
import com.atguigu.result.RetVal;
import com.atguigu.service.UserInfoService;
import com.atguigu.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author GodWei
 * @since 2022-08-06
 */
@RestController
@RequestMapping("/user")
public class UserInfoController {
    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private RedisTemplate redisTemplate;

    //1.登录的认证逻辑
    @PostMapping("login")
    public RetVal login(@RequestBody UserInfo uiUserInfo, HttpServletRequest request) {
        // 1.1 从数据库获取uiUserInfo对应的userInfo
        UserInfo dbUser = userInfoService.queryUserFromDb(uiUserInfo);

        if (dbUser != null) {
            Map<String, Object> retMap = new HashMap<>();

            //todo  有空搞明白为什么是 token 和 nickname
            String token = UUID.randomUUID().toString();
            retMap.put("token", token);
            //返回用户昵称给前端
            String nickName = dbUser.getNickName();
            retMap.put("nickName", nickName);
            //将用户信息存储到redis中
            String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
            JSONObject userInfoObject = new JSONObject();
            userInfoObject.put("userId", dbUser.getId());
            userInfoObject.put("loginIp", IpUtil.getIpAddress(request));
            redisTemplate.opsForValue().set(userKey, userInfoObject, RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);

            return RetVal.ok(retMap);


        }
        return RetVal.fail().message("登录失败");
    }

    //2. 登出的业务逻辑
    @GetMapping("logout")
    public RetVal logout(HttpServletRequest request) {
        String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + request.getHeader("token");
        redisTemplate.delete(userKey);
        return RetVal.ok();
    }

}

