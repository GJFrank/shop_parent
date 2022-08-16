package com.atguigu.mytest;

import com.atguigu.result.RetVal;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.PrivateKey;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/7/28 19:01 周四
 * description:
 */
@Api(value = "缓存测试接口")
@RestController
@RequestMapping("product/test")
public class TestController {
    @Autowired
    private TestService testService;

    @GetMapping("testLock")
    public RetVal testLock() {
        testService.testLock();
        return RetVal.ok();
    }

    @GetMapping("readLock")
    public RetVal<String> readLock(){

       String msg= testService.readLock();
       return RetVal.ok(msg);
    }

    @GetMapping("writeLock")
    public RetVal<String> writeLock(){

        String msg= testService.writeLock();
        return RetVal.ok(msg);
    }
}
