package com.atguigu.controller;


import com.alibaba.fastjson.JSONObject;
import com.atguigu.entity.BaseCategory1;
import com.atguigu.entity.BaseCategory2;
import com.atguigu.entity.BaseCategory3;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.result.RetVal;
import com.atguigu.service.BaseCategory1Service;
import com.atguigu.service.BaseCategory2Service;
import com.atguigu.service.BaseCategory3Service;
import com.atguigu.service.BaseCategoryViewService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 一级分类表 前端控制器
 * </p>
 *
 * @author GodWei
 * @since 2022-07-20
 */
@RestController
@RequestMapping("/product")
//@CrossOrigin
public class CategoryController {
    @Autowired
    private BaseCategory1Service category1Service;
    @Autowired
    private BaseCategory2Service category2Service;
    @Autowired
    private BaseCategory3Service category3Service;
    @Autowired
    private BaseCategoryViewService categoryViewService;

    @GetMapping("getCategory2/{category1Id}")
    public RetVal getCategory2(@PathVariable Long category1Id) {
        QueryWrapper<BaseCategory2> wrapper = new QueryWrapper<>();
        wrapper.eq("category1_id", category1Id);
        List<BaseCategory2> category2List = category2Service.list(wrapper);
        return RetVal.ok(category2List);
    }

    @GetMapping("getCategory3/{category2Id}")
    public RetVal getCategory3(@PathVariable Long category2Id) {
        QueryWrapper<BaseCategory3> wrapper = new QueryWrapper<>();
        wrapper.eq("category2_id", category2Id);
        List<BaseCategory3> category3List = category3Service.list(wrapper);
        return RetVal.ok(category3List);
    }

    @GetMapping("getCategory1")
    public RetVal getCategory1() {
        List<BaseCategory1> category1List = category1Service.list(null);
        return RetVal.ok(category1List);
    }

    @GetMapping("getIndexCategory")
    public RetVal getIndexCategory() {
        List<JSONObject> categoryList = categoryViewService.getIndexCategory();
        return RetVal.ok(categoryList);
    }
}

