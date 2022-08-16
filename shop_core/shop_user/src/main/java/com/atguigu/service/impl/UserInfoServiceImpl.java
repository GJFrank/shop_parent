package com.atguigu.service.impl;

import com.atguigu.entity.UserInfo;
import com.atguigu.mapper.UserInfoMapper;
import com.atguigu.service.UserInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author GodWei
 * @since 2022-08-06
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Override
    public UserInfo queryUserFromDb(UserInfo uiUserInfo) {
        QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("login_name", uiUserInfo.getLoginName());
        String passwd = uiUserInfo.getPasswd();
        String encodedPasswd = DigestUtils.md5DigestAsHex(passwd.getBytes());
        wrapper.eq("passwd", encodedPasswd);
        return baseMapper.selectOne(wrapper);
    }
}
