package com.atguigu.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.dao.SearchMapper;
import com.atguigu.entity.BaseBrand;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.entity.PlatformPropertyKey;
import com.atguigu.entity.SkuInfo;
import com.atguigu.search.*;
import com.atguigu.service.SearchService;
import lombok.SneakyThrows;
import org.apache.lucene.search.join.ScoreMode;
import org.aspectj.weaver.ast.Var;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.lucene.search.function.FunctionScoreQuery;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Reference;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;
import com.atguigu.client.ProductFeignClient;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/8/3 13:25 周三
 * description:
 */
@Service
public class SearchServiceImpl implements SearchService {
    /**
     * 商品上架 , 从数据库加入es
     *
     * @param skuId
     */
    @Autowired
    private SearchMapper searchMapper;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override
    public void onSale(Long skuId) {
        //参数校验
        if (skuId == null) {
            return;
        }
        //初始化 product对象
        Product productSearch = new Product();
        // 商品Id
        // private Long id;
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        if (skuInfo == null || skuInfo.getId() == null) {
            return;
        }
        //补全信息
        productSearch.setId(skuInfo.getId());
        // 商品的默认图片
        // private String defaultImage;
        productSearch.setDefaultImage(skuInfo.getSkuDefaultImg());
        // 商品名称
        //  private String productName;
        productSearch.setProductName(skuInfo.getSkuName());

        // 商品价格
        // private Double price;
        productSearch.setPrice(productFeignClient.getSkuPrice(skuId).doubleValue());

        // 创建时间
        // private Date createTime; // 新品
        productSearch.setCreateTime(new Date());

        // 品牌Id //  private Long brandId;
        // 品牌名称 // private String brandName;
        // 品牌logo //private String brandLogoUrl;
        Long brandId = skuInfo.getBrandId();
        productSearch.setBrandId(brandId);

        BaseBrand brand = productFeignClient.getBrandById(brandId);
        productSearch.setBrandName(brand.getBrandName());
        productSearch.setBrandLogoUrl(brand.getBrandLogoUrl());

        //private Long category1Id;
        // private String category1Name;
        //  private Long category2Id;
        // private String category2Name;
        //  private Long category3Id;
        // private String category3Name;
        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        if (categoryView != null) {
            productSearch.setCategory1Id(categoryView.getCategory1Id());
            productSearch.setCategory1Name(categoryView.getCategory1Name());
            productSearch.setCategory2Id(categoryView.getCategory2Id());
            productSearch.setCategory2Name(categoryView.getCategory2Name());
            productSearch.setCategory3Id(categoryView.getCategory3Id());
            productSearch.setCategory3Name(categoryView.getCategory3Name());
        }

        //热度排名
        // private Long hotScore = 0L;

        //平台属性集合对象  //Nested支持嵌套查询
        // private List<SearchPlatformProperty> platformProperty;
        List<PlatformPropertyKey> platformPropertyKeyList = productFeignClient.getPlatformBySkuId(skuId);
        if (!CollectionUtils.isEmpty(platformPropertyKeyList)) {
            List<SearchPlatformProperty> searchPlatformPropertyList = platformPropertyKeyList.stream().map(
                    platformPropertyKey -> {
                        SearchPlatformProperty searchPlatformProperty = new SearchPlatformProperty();
                        //平台属性id
                        searchPlatformProperty.setPropertyKeyId(platformPropertyKey.getId());
                        // 平台属性 value
                        String propertyValue = platformPropertyKey.getPropertyValueList().get(0).getPropertyValue();
                        searchPlatformProperty.setPropertyValue(propertyValue);
                        //平台属性key
                        searchPlatformProperty.setPropertyKey(platformPropertyKey.getPropertyKey());
                        return searchPlatformProperty;
                    }

            ).collect(Collectors.toList());
            productSearch.setPlatformProperty(searchPlatformPropertyList);
        }

        //保存商品 到es中
        searchMapper.save(productSearch);
    }

    @Override
    public void offSale(Long skuId) {
        searchMapper.deleteById(skuId);

    }

    //将当前方法抛出的异常, 伪装成RuntimeException, 骗过编译器, 是的不显式处理异常信息
    @SneakyThrows
    @Override
    public SearchResponseVo searchProduct(SearchParam searchParam) {
        //1. 生成商品DSL语句
        SearchRequest searchRequest = buildQueryDsl(searchParam);
        //2. 实现对DSL语句的调用
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        //3.把查询出来的结果封装解析起来  //经过3后, 有部分数据已填充
        SearchResponseVo searchResponseVo = parseSearchResult(searchResponse);
        //4.还需要设置其他参数
        //每页显示的内容 pageSize
        //当前页面 pageNo
        // totalPages
        Integer pageNo = searchParam.getPageNo();
        Integer pageSize = searchParam.getPageSize();
        searchResponseVo.setPageNo(pageNo);
        searchResponseVo.setPageSize(pageSize);

        long totalPages = 0;
        Integer total = searchResponseVo.getTotal().intValue();
        if (total % pageSize == 0) {
            totalPages = (total / pageSize);
        } else {
            totalPages = (total / pageSize) + 1;
        }
        searchResponseVo.setTotalPages(totalPages);

        return searchResponseVo;
    }

    //2. 把查询出来的结果解析封装起来
    private SearchResponseVo parseSearchResult(SearchResponse searchResponse) {
        // ** 需要返回接受数据的对象
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        //1. 拿到商品的基本信息
        SearchHits firstHit = searchResponse.getHits();
        //总记录数
        long totalHits = firstHit.getTotalHits();
        searchResponseVo.setTotal(totalHits);
        // 获取第二个hit里面的数据
        SearchHit[] secondHits = firstHit.getHits();
        if (secondHits != null && secondHits.length > 0) {
            //商品的基本信息
            for (SearchHit secondHit : secondHits) {
                Product productSearch = JSONObject.parseObject(secondHit.getSourceAsString(), Product.class);

                //拿到高亮里面的productName
                HighlightField highlightField = secondHit.getHighlightFields().get("productName");
                if (highlightField != null) {
                    Text highlightFieldFragment = highlightField.getFragments()[0];
                    productSearch.setProductName(highlightFieldFragment.toString());
                }
                searchResponseVo.getProductList().add(productSearch);
            }
        }
        //2. 拿到商品的品牌信息
        ParsedLongTerms brandIdAgg = searchResponse.getAggregations().get("brandIdAgg");
        List<SearchBrandVo> searchBrandVoList = brandIdAgg.getBuckets().stream().map(bucket -> {
            SearchBrandVo searchBrandVo = new SearchBrandVo();
            //2.1 品牌Id
            String brandId = bucket.getKeyAsString();
            searchBrandVo.setBrandId(Long.parseLong(brandId));

            //2.2 品牌名称
            ParsedStringTerms brandNameAgg = bucket.getAggregations().get("brandNameAgg");
            //错误点: size为0 的原因  构建的时候 写错了名词
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            searchBrandVo.setBrandName(brandName);

            //2.3 图片地址
            ParsedStringTerms brandLogoUrlAgg = bucket.getAggregations().get("brandLogoUrlAgg");
            String brandLogoUrl = brandLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchBrandVo.setBrandLogoUrl(brandLogoUrl);
            return searchBrandVo;

        }).collect(Collectors.toList());
        searchResponseVo.setBrandVoList(searchBrandVoList);

        //3. 拿到商品的平台聚合信息
        ParsedNested platformPropertyAgg = searchResponse.getAggregations().get("platformPropertyAgg");
        ParsedLongTerms propertyKeyIdAgg = platformPropertyAgg.getAggregations().get("propertyKeyIdAgg");
        List<SearchPlatformPropertyVo> platformPropertyVoList = propertyKeyIdAgg.getBuckets().stream().map(
                bucket -> {
                    //创建SearchPlatformPropertyVo 接受结果
                    SearchPlatformPropertyVo searchPlatformPropertyVo = new SearchPlatformPropertyVo();
                    //平台属性id
                    Number propertyKeyId = bucket.getKeyAsNumber();
                    searchPlatformPropertyVo.setPropertyKeyId(propertyKeyId.longValue());

                    //平台属性key
                    ParsedStringTerms propertyKeyAgg = bucket.getAggregations().get("propertyKeyAgg");
                    String propertyKey = propertyKeyAgg.getBuckets().get(0).getKeyAsString();
                    searchPlatformPropertyVo.setPropertyKey(propertyKey);

                    //平台属性value
                    ParsedStringTerms propertyValueAgg = ((Terms.Bucket) bucket).getAggregations().get("propertyValueAgg");
                    List<String> propertyValueList = propertyValueAgg.getBuckets().stream().map(
                            Terms.Bucket::getKeyAsString
                    ).collect(Collectors.toList());
                    searchPlatformPropertyVo.setPropertyValueList(propertyValueList);
                    return searchPlatformPropertyVo;
                }

        ).collect(Collectors.toList());
        searchResponseVo.setPlatformPropertyList(platformPropertyVoList);


        return searchResponseVo;
    }

    //1. 生成商品搜索DSL语句
    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        //1. 构造一个query
        SearchSourceBuilder esBuilder = new SearchSourceBuilder();
        //2. 构造一个bool
        BoolQueryBuilder firstBool = QueryBuilders.boolQuery();
        //3. 构造一个filter
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())) {
            //构造一级分类过滤器
            TermQueryBuilder category1Id = QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id());
            firstBool.filter(category1Id);
        }
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())) {
            //构造二级分类过滤器
            TermQueryBuilder category2Id = QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id());
            firstBool.filter(category2Id);
        }
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())) {
            //3. 构造三级分类过滤器
            TermQueryBuilder category3Id = QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id());
            firstBool.filter(category3Id);
        }

        String brandName = searchParam.getBrandName();
        if (!StringUtils.isEmpty(brandName)) {
            // 4. 构造一个品牌过滤器
            String[] brandParam = brandName.split(":");
            if (brandParam.length == 2) {
                firstBool.filter(QueryBuilders.termQuery("brandId", brandParam[0]));
            }
        }

        //5. 构造关键字查询
        String keyword = searchParam.getKeyword();
        if (!StringUtils.isEmpty(keyword)) {
            MatchQueryBuilder matchQuery = QueryBuilders.matchQuery("productName", keyword).operator(Operator.AND);
            firstBool.must(matchQuery);
        }
        //6.构造平台属性过滤器 &props=4:骁龙888:CPU型号&&props=5:6.0～6.24英寸:屏幕尺寸
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0) {
            for (String prop : props) {
                String[] platformParams = prop.split(":");
                if (platformParams.length == 3) {
                    BoolQueryBuilder secondBool = QueryBuilders.boolQuery();
                    BoolQueryBuilder childBool = QueryBuilders.boolQuery();
                    childBool.must(QueryBuilders.termQuery("platformProperty.propertyKeyId", platformParams[0]));
                    childBool.must(QueryBuilders.termQuery("platformProperty.propertyValue", platformParams[1]));
                    secondBool.must(QueryBuilders.nestedQuery("platformProperty", childBool, ScoreMode.None));
                    firstBool.filter(secondBool);
                }
            }
        }

        esBuilder.query(firstBool);
        //7. 构造分页
        int from = (searchParam.getPageNo() - 1) * searchParam.getPageSize();
        esBuilder.from(from);
        esBuilder.size(searchParam.getPageSize());

        //8. 构造排序
        // --1 综合排序 hotScore
        // --2 价格排序 price
        String pageOrder = searchParam.getOrder();
        if (!StringUtils.isEmpty(pageOrder)) {
            String[] orderParam = pageOrder.split(":");
            if (orderParam.length == 2) {
                String fileName = "";
                switch (orderParam[0]) {
                    case "1":
                        fileName = "hotScore";
                        break;
                    case "2":
                        fileName = "price";
                        break;
                }
                esBuilder.sort(fileName, "asc".equals(orderParam[1]) ? SortOrder.ASC : SortOrder.DESC);
            }

        } else {
            //如果没有选择排序方式, 选择默认方式
            esBuilder.sort("hotScore", SortOrder.ASC);
        }
        //9. 构造高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("productName");
        highlightBuilder.preTags("<span style=color:red>");
        highlightBuilder.postTags("</span>");
        esBuilder.highlighter(highlightBuilder);

        //构造品牌聚合
        TermsAggregationBuilder brandIdAggBuilder = AggregationBuilders.terms("brandIdAgg").field("brandId").
                subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("brandLogoUrlAgg").field("brandLogoUrl"));
        esBuilder.aggregation(brandIdAggBuilder);

        //构造平台属性聚合
        esBuilder.aggregation(AggregationBuilders.nested("platformPropertyAgg", "platformProperty").
                subAggregation(AggregationBuilders.terms("propertyKeyIdAgg").field("platformProperty.propertyKeyId").
                        subAggregation(AggregationBuilders.terms("propertyKeyAgg").field("platformProperty.propertyKey")).
                        subAggregation(AggregationBuilders.terms("propertyValueAgg").field("platformProperty.propertyValue"))));

        //12. 查询哪个index和type
        SearchRequest searchRequest = new SearchRequest("product");
        searchRequest.types("info");
        searchRequest.source(esBuilder);
        System.out.println("拼接好的dsl语句=  " + esBuilder.toString());
        return searchRequest;

    }
}
