package com.atguigu.controller;


import com.atguigu.entity.BaseBrand;
import com.atguigu.result.RetVal;
import com.atguigu.service.BaseBrandService;
import com.atguigu.utils.MinioUploader;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 * 品牌表 前端控制器
 * </p>
 *
 * @author GodWei
 * @since 2022-07-23
 */
@RestController
@RequestMapping("/product/brand")
public class BrandController {
    //注入service
    @Autowired
    private BaseBrandService brandService;
    @Autowired
    private MinioUploader minioUploader;

    // 1. 查- 分页查询 http://127.0.0.1/product/brand/queryBrandByPage/1/10
    @GetMapping("queryBrandByPage/{currentPageNum}/{pageSize}")
    public RetVal queryBrandByPage(@PathVariable Long currentPageNum, @PathVariable Long pageSize) {
        Page<BaseBrand> page = new Page<>(currentPageNum, pageSize);
        brandService.page(page, null);
        return RetVal.ok(page);
    }

    // 2. 增
    @PostMapping
    public RetVal saveBrand(@RequestBody BaseBrand brand) {
        brandService.save(brand);
        return RetVal.ok();
    }

    //3.根据id查询品牌信息
    //http://127.0.0.1/product/brand/4
    @GetMapping("{brandId}")
    public RetVal getById(@PathVariable Long brandId) {
        BaseBrand brand = brandService.getById(brandId);
        return RetVal.ok(brand);
    }

    //4.更新品牌信息
    @PutMapping
    public RetVal updateBrand(@RequestBody BaseBrand brand) {
        brandService.updateById(brand);
        return RetVal.ok();
    }

    //5.删除品牌信息
    @DeleteMapping("{brandId}")
    public RetVal remove(@PathVariable Long brandId) {
        brandService.removeById(brandId);
        return RetVal.ok();
    }

    //6.查询所有的品牌
    @GetMapping("getAllBrand")
    public RetVal getAllBrand() {
        List<BaseBrand> brandList = brandService.list(null);
        return RetVal.ok(brandList);
    }

    //7. 上传文件的接口
    @PostMapping("fileUpload")
    public RetVal fileUpload(MultipartFile file) throws Exception {
        String retUrl = minioUploader.uploadFile(file);
        return RetVal.ok(retUrl);
    }

    //8.根据id查询品牌信息
    //http://127.0.0.1/product/brand/4
    @GetMapping("getBrandById/{brandId}")
    public BaseBrand getBrandById(@PathVariable Long brandId) {
        BaseBrand brand = brandService.getById(brandId);
        return brand;
    }

}

