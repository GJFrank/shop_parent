package com.atguigu.service;

import com.atguigu.search.SearchParam;
import com.atguigu.search.SearchResponseVo;

public interface SearchService {
    /**
     * 商品上架 , 从数据库加入es
     *
     * @param skuId
     */
    public void onSale(Long skuId);

    /**
     * 商品下架, 从es删除商品
     *
     * @param skuId
     */
    public void offSale(Long skuId);

    SearchResponseVo searchProduct(SearchParam searchParam);
}
