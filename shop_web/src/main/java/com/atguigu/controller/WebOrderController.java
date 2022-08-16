package com.atguigu.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.client.OrderFeignClient;
import com.atguigu.client.ProductFeignClient;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import com.atguigu.result.RetVal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/7/26 11:05 周二
 * description:
 */
@Controller
public class WebOrderController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    @RequestMapping("confirm.html")
    public String confirm(Model model) {
        RetVal<Map<String, Object>> retVal = orderFeignClient.confirm();
        model.addAllAttributes(retVal.getData());
        return "order/confirm";

    }
}
