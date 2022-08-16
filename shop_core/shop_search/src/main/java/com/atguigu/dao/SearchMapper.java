package com.atguigu.dao;

import com.atguigu.search.Product;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/8/3 13:26 周三
 * description:
 */

public interface SearchMapper  extends ElasticsearchRepository<Product,Long> {
}
