package com.atguigu.controller;

import com.atguigu.result.RetVal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/8/3 13:58 周三
 * description:
 */

@RestController
@RequestMapping("/test")

public class TESTController {
    @RequestMapping("/first")
    public RetVal firstTest(){
        System.out.println("po");
        return RetVal.ok();
    }
}
