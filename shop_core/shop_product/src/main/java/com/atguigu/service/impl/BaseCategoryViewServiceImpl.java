package com.atguigu.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.mapper.BaseCategoryViewMapper;
import com.atguigu.service.BaseCategoryViewService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * VIEW 服务实现类
 * </p>
 *
 * @author GodWei
 * @since 2022-07-25
 */
@Service
public class BaseCategoryViewServiceImpl extends ServiceImpl<BaseCategoryViewMapper, BaseCategoryView> implements BaseCategoryViewService {

    @Override
    public List<JSONObject> getIndexCategory() {
        //b. 查询所有的分类信息
        List<BaseCategoryView> categoryViewList = baseMapper.selectList(null);
        //c. 找到所有的一级分类
        Map<Long, List<BaseCategoryView>> category1Map = categoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        // 所有的一级分类json数据
        List<JSONObject> allCategoryJson = new ArrayList<>();
        Integer index = 0;
        for (Map.Entry<Long, List<BaseCategoryView>> category1Entry : category1Map.entrySet()) {
            Long category1Id = category1Entry.getKey();
            List<BaseCategoryView> category1List = category1Entry.getValue();
            //构造一个json格式数据(一级分类)
            JSONObject category1Json = new JSONObject();
            category1Json.put("index",++index);
            category1Json.put("categoryId",category1Id);
            category1Json.put("categoryName",category1List.get(0).getCategory1Name());
           // System.out.println("category1Json = " + category1Json);
            //d. 找到所有二级分类
            List<JSONObject> category1Children = new ArrayList<>();
            Map<Long, List<BaseCategoryView>> category2Map = category1List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            category2Map.entrySet().forEach(
                    category2Entry->{
                        Long category2Id = category2Entry.getKey();
                        List<BaseCategoryView> category2List = category2Entry.getValue();
                        JSONObject category2Json=new JSONObject();
                        category2Json.put("categoryId",category2Id);
                        category2Json.put("categoryName",category2List.get(0).getCategory2Name());

                        //e. 找到所有三级分类
                        ArrayList<JSONObject> category2Children = new ArrayList<>();
                        Map<Long, List<BaseCategoryView>> category3Map = category2List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory3Id));
                        category3Map.entrySet().forEach(
                                category3Entry->{
                                    Long category3Id = category3Entry.getKey();
                                    List<BaseCategoryView> category3List = category3Entry.getValue();
                                    JSONObject category3Json = new JSONObject();
                                    category3Json.put("categoryName",category3List.get(0).getCategory3Name());
                                    category3Json.put("categoryId",category3Id);
                                    category2Children.add(category3Json);
                                }
                        );
                        category2Json.put("categoryChild",category2Children);
                       // System.out.println("category3Map = " + category3Map);
                        category1Children.add(category2Json);
                    }
            );
            //
            category1Json.put("categoryChild",category1Children);
            //System.out.println("category1Json = " + category1Json);
            allCategoryJson.add(category1Json);
        }

        return allCategoryJson;

    }
}
