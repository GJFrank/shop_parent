package com.atguigu.controller;

import com.atguigu.client.SeckillFeignClient;
import com.atguigu.entity.SeckillProduct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/8/15 13:43 周一
 * description:
 */
@Controller
public class WebSeckillController {
    @Autowired
    private SeckillFeignClient seckillFeignClient;

    //1. 查询所有秒杀商品
    @GetMapping("/seckill-index.html")
    public String seckillIndex(Model model) {
        List<SeckillProduct> seckillProductList = seckillFeignClient.queryAllSecKillProduct();
        model.addAttribute("list", seckillProductList);
        return "seckill/index";
    }

    // 2. 根据skuId 获取秒杀对象数据
    @GetMapping("/seckill-detail/{skuId}.html")
    public String seckilDetail(@PathVariable Long skuId, Model model) {
        SeckillProduct seckillProduct = seckillFeignClient.querySecKillProductById(skuId);
        model.addAttribute("item", seckillProduct);
        return "seckill/detail";
    }

    // 3. 获取抢购码成功之后的页面 seckill-queue.html?skuId=33&seckillCode=eccbc87e4b5ce2fe28308fd9f2a7baf3
    @GetMapping("/seckill-queue.html")
    public String seckillQueue(Long skuId, String seckillCode, HttpServletRequest request) {
        request.setAttribute("skuId",skuId);
        request.setAttribute("seckillCode",seckillCode);
        return "seckill/queue";
    }
}
