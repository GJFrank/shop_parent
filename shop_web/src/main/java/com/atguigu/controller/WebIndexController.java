package com.atguigu.controller;

import com.atguigu.client.ProductFeignClient;
import com.atguigu.client.SearchFeignClient;
import com.atguigu.result.RetVal;
import com.atguigu.search.SearchParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/8/2 10:24 周二
 * description:
 */
@Controller
public class WebIndexController {
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private SearchFeignClient searchFeignClient;

    @RequestMapping({"/", "index.html"})
    public String index(Model model) {
        RetVal indexCategory = productFeignClient.getIndexCategory();
        model.addAttribute("list", indexCategory.getData());
        return "index/index";
    }

    @GetMapping("search.html")
    public String searchProduct(SearchParam searchParam, Model model) {
        RetVal<Map> retVal = searchFeignClient.searchProduct(searchParam);
        // 页面设置搜索到的商品信息
        model.addAllAttributes(retVal.getData());
        // 前台需要存储一个searchParam
     //   model.addAttribute("searchParam", searchParam);
        //1. 设置搜索路径
        String urlParam = pageUrlParam(searchParam);
        model.addAttribute("urlParam", urlParam);
        //2. 设置品牌显示
        String brandNameParam = pageBrandNameParam(searchParam.getBrandName());
        model.addAttribute("brandNameParam", brandNameParam);
        //3.设置平台属性显示
        List<Map<String, String>> propertyList = pagePlatformParam(searchParam.getProps());
        model.addAttribute("propsParamList", propertyList);

        //4. 设置排序信息显示
        Map<String, String> map = pageSortInfo(searchParam.getOrder());
        model.addAttribute("orderMap", map);

        return "search/index";
    }

    private Map<String, String> pageSortInfo(String order) {
        Map<String, String> orderMap = new HashMap<>();
        if (!StringUtils.isEmpty(order)) {
            String[] orderSplit = order.split(":");
            if (orderSplit.length == 2) {
                orderMap.put("type", orderSplit[0]);
                orderMap.put("sort", orderSplit[1]);
            }
        } else {
            orderMap.put("type", "1");
            orderMap.put("sort", "desc");
        }
        return orderMap;
    }

    private List pagePlatformParam(String[] props) {
        List<Map<String, String>> list = new ArrayList<>();
        if (props != null && props.length > 0) {
            for (String prop : props) {
                String[] propSplit = prop.split(":");
                if (propSplit.length == 3) {
                    Map<String, String> propMap = new HashMap<>();
                    propMap.put("propertyKeyId", propSplit[0]);
                    propMap.put("propertyValue", propSplit[1]);
                    propMap.put("propertyKey", propSplit[2]);
                    list.add(propMap);
                }
            }
        }
        return list;
    }

    private String pageBrandNameParam(String brandName) {
        if (!StringUtils.isEmpty(brandName)) {
            String[] brandSplit = brandName.split(":");
            if (brandSplit.length == 2) {
                return "品牌:" + brandSplit[1];
            }
        }
        return null;
    }

    private String pageUrlParam(SearchParam searchParam) {
        StringBuilder urlParam = new StringBuilder();

        //keyword关键字非空判断
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            urlParam.append("keyword=").append(searchParam.getKeyword());
        }
        //判断是否有品牌 &brandName=3:三星
        if (!StringUtils.isEmpty(searchParam.getBrandName())) {
            if (urlParam.length() > 0) {
                urlParam.append("&brandName=").append(searchParam.getBrandName());
            }
        }
        //判断是否有平台属性 &props=5:5.0英寸以下:屏幕尺寸&props=4:骁龙888:CPU型号
        if (!StringUtils.isEmpty(searchParam.getProps())) {
            if (urlParam.length() > 0) {
                for (String prop : searchParam.getProps()) {
                    urlParam.append("&props=").append(prop);
                }
            }
        }
        return "search.html?" + urlParam.toString();
    }

}
