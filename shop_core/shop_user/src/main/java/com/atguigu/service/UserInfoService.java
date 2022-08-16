package com.atguigu.service;

import com.atguigu.entity.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author GodWei
 * @since 2022-08-06
 */
public interface UserInfoService extends IService<UserInfo> {

    UserInfo queryUserFromDb(UserInfo uiUserInfo);
}
