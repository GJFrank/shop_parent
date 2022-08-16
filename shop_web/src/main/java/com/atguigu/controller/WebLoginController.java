package com.atguigu.controller;

import com.atguigu.entity.UserInfo;
import com.atguigu.result.RetVal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/8/6 16:48 周六
 * description:
 */
@Controller
public class WebLoginController {
//    @Autowired

    @RequestMapping("login.html")
    public String login(Model model, HttpServletRequest request) {
        //originalUrl记录用户从什么位置点击到登录的
        String originalUrl = request.getParameter("originalUrl");
        //需要后台存储originalUrl, 因为页面需要
        model.addAttribute("originalUrl", originalUrl);
        //从数据库获取用户的数据
        return "login";
    }
}
