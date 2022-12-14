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
 * @author: GOD???
 * time: 2022/8/3 13:25 ??????
 * description:
 */
@Service
public class SearchServiceImpl implements SearchService {
    /**
     * ???????????? , ??????????????????es
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
        //????????????
        if (skuId == null) {
            return;
        }
        //????????? product??????
        Product productSearch = new Product();
        // ??????Id
        // private Long id;
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        if (skuInfo == null || skuInfo.getId() == null) {
            return;
        }
        //????????????
        productSearch.setId(skuInfo.getId());
        // ?????????????????????
        // private String defaultImage;
        productSearch.setDefaultImage(skuInfo.getSkuDefaultImg());
        // ????????????
        //  private String productName;
        productSearch.setProductName(skuInfo.getSkuName());

        // ????????????
        // private Double price;
        productSearch.setPrice(productFeignClient.getSkuPrice(skuId).doubleValue());

        // ????????????
        // private Date createTime; // ??????
        productSearch.setCreateTime(new Date());

        // ??????Id //  private Long brandId;
        // ???????????? // private String brandName;
        // ??????logo //private String brandLogoUrl;
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

        //????????????
        // private Long hotScore = 0L;

        //????????????????????????  //Nested??????????????????
        // private List<SearchPlatformProperty> platformProperty;
        List<PlatformPropertyKey> platformPropertyKeyList = productFeignClient.getPlatformBySkuId(skuId);
        if (!CollectionUtils.isEmpty(platformPropertyKeyList)) {
            List<SearchPlatformProperty> searchPlatformPropertyList = platformPropertyKeyList.stream().map(
                    platformPropertyKey -> {
                        SearchPlatformProperty searchPlatformProperty = new SearchPlatformProperty();
                        //????????????id
                        searchPlatformProperty.setPropertyKeyId(platformPropertyKey.getId());
                        // ???????????? value
                        String propertyValue = platformPropertyKey.getPropertyValueList().get(0).getPropertyValue();
                        searchPlatformProperty.setPropertyValue(propertyValue);
                        //????????????key
                        searchPlatformProperty.setPropertyKey(platformPropertyKey.getPropertyKey());
                        return searchPlatformProperty;
                    }

            ).collect(Collectors.toList());
            productSearch.setPlatformProperty(searchPlatformPropertyList);
        }

        //???????????? ???es???
        searchMapper.save(productSearch);
    }

    @Override
    public void offSale(Long skuId) {
        searchMapper.deleteById(skuId);

    }

    //??????????????????????????????, ?????????RuntimeException, ???????????????, ?????????????????????????????????
    @SneakyThrows
    @Override
    public SearchResponseVo searchProduct(SearchParam searchParam) {
        //1. ????????????DSL??????
        SearchRequest searchRequest = buildQueryDsl(searchParam);
        //2. ?????????DSL???????????????
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        //3.??????????????????????????????????????????  //??????3???, ????????????????????????
        SearchResponseVo searchResponseVo = parseSearchResult(searchResponse);
        //4.???????????????????????????
        //????????????????????? pageSize
        //???????????? pageNo
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

    //2. ??????????????????????????????????????????
    private SearchResponseVo parseSearchResult(SearchResponse searchResponse) {
        // ** ?????????????????????????????????
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        //1. ???????????????????????????
        SearchHits firstHit = searchResponse.getHits();
        //????????????
        long totalHits = firstHit.getTotalHits();
        searchResponseVo.setTotal(totalHits);
        // ???????????????hit???????????????
        SearchHit[] secondHits = firstHit.getHits();
        if (secondHits != null && secondHits.length > 0) {
            //?????????????????????
            for (SearchHit secondHit : secondHits) {
                Product productSearch = JSONObject.parseObject(secondHit.getSourceAsString(), Product.class);

                //?????????????????????productName
                HighlightField highlightField = secondHit.getHighlightFields().get("productName");
                if (highlightField != null) {
                    Text highlightFieldFragment = highlightField.getFragments()[0];
                    productSearch.setProductName(highlightFieldFragment.toString());
                }
                searchResponseVo.getProductList().add(productSearch);
            }
        }
        //2. ???????????????????????????
        ParsedLongTerms brandIdAgg = searchResponse.getAggregations().get("brandIdAgg");
        List<SearchBrandVo> searchBrandVoList = brandIdAgg.getBuckets().stream().map(bucket -> {
            SearchBrandVo searchBrandVo = new SearchBrandVo();
            //2.1 ??????Id
            String brandId = bucket.getKeyAsString();
            searchBrandVo.setBrandId(Long.parseLong(brandId));

            //2.2 ????????????
            ParsedStringTerms brandNameAgg = bucket.getAggregations().get("brandNameAgg");
            //?????????: size???0 ?????????  ??????????????? ???????????????
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            searchBrandVo.setBrandName(brandName);

            //2.3 ????????????
            ParsedStringTerms brandLogoUrlAgg = bucket.getAggregations().get("brandLogoUrlAgg");
            String brandLogoUrl = brandLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchBrandVo.setBrandLogoUrl(brandLogoUrl);
            return searchBrandVo;

        }).collect(Collectors.toList());
        searchResponseVo.setBrandVoList(searchBrandVoList);

        //3. ?????????????????????????????????
        ParsedNested platformPropertyAgg = searchResponse.getAggregations().get("platformPropertyAgg");
        ParsedLongTerms propertyKeyIdAgg = platformPropertyAgg.getAggregations().get("propertyKeyIdAgg");
        List<SearchPlatformPropertyVo> platformPropertyVoList = propertyKeyIdAgg.getBuckets().stream().map(
                bucket -> {
                    //??????SearchPlatformPropertyVo ????????????
                    SearchPlatformPropertyVo searchPlatformPropertyVo = new SearchPlatformPropertyVo();
                    //????????????id
                    Number propertyKeyId = bucket.getKeyAsNumber();
                    searchPlatformPropertyVo.setPropertyKeyId(propertyKeyId.longValue());

                    //????????????key
                    ParsedStringTerms propertyKeyAgg = bucket.getAggregations().get("propertyKeyAgg");
                    String propertyKey = propertyKeyAgg.getBuckets().get(0).getKeyAsString();
                    searchPlatformPropertyVo.setPropertyKey(propertyKey);

                    //????????????value
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

    //1. ??????????????????DSL??????
    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        //1. ????????????query
        SearchSourceBuilder esBuilder = new SearchSourceBuilder();
        //2. ????????????bool
        BoolQueryBuilder firstBool = QueryBuilders.boolQuery();
        //3. ????????????filter
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())) {
            //???????????????????????????
            TermQueryBuilder category1Id = QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id());
            firstBool.filter(category1Id);
        }
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())) {
            //???????????????????????????
            TermQueryBuilder category2Id = QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id());
            firstBool.filter(category2Id);
        }
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())) {
            //3. ???????????????????????????
            TermQueryBuilder category3Id = QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id());
            firstBool.filter(category3Id);
        }

        String brandName = searchParam.getBrandName();
        if (!StringUtils.isEmpty(brandName)) {
            // 4. ???????????????????????????
            String[] brandParam = brandName.split(":");
            if (brandParam.length == 2) {
                firstBool.filter(QueryBuilders.termQuery("brandId", brandParam[0]));
            }
        }

        //5. ?????????????????????
        String keyword = searchParam.getKeyword();
        if (!StringUtils.isEmpty(keyword)) {
            MatchQueryBuilder matchQuery = QueryBuilders.matchQuery("productName", keyword).operator(Operator.AND);
            firstBool.must(matchQuery);
        }
        //6.??????????????????????????? &props=4:??????888:CPU??????&&props=5:6.0???6.24??????:????????????
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
        //7. ????????????
        int from = (searchParam.getPageNo() - 1) * searchParam.getPageSize();
        esBuilder.from(from);
        esBuilder.size(searchParam.getPageSize());

        //8. ????????????
        // --1 ???????????? hotScore
        // --2 ???????????? price
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
            //??????????????????????????????, ??????????????????
            esBuilder.sort("hotScore", SortOrder.ASC);
        }
        //9. ????????????
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("productName");
        highlightBuilder.preTags("<span style=color:red>");
        highlightBuilder.postTags("</span>");
        esBuilder.highlighter(highlightBuilder);

        //??????????????????
        TermsAggregationBuilder brandIdAggBuilder = AggregationBuilders.terms("brandIdAgg").field("brandId").
                subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("brandLogoUrlAgg").field("brandLogoUrl"));
        esBuilder.aggregation(brandIdAggBuilder);

        //????????????????????????
        esBuilder.aggregation(AggregationBuilders.nested("platformPropertyAgg", "platformProperty").
                subAggregation(AggregationBuilders.terms("propertyKeyIdAgg").field("platformProperty.propertyKeyId").
                        subAggregation(AggregationBuilders.terms("propertyKeyAgg").field("platformProperty.propertyKey")).
                        subAggregation(AggregationBuilders.terms("propertyValueAgg").field("platformProperty.propertyValue"))));

        //12. ????????????index???type
        SearchRequest searchRequest = new SearchRequest("product");
        searchRequest.types("info");
        searchRequest.source(esBuilder);
        System.out.println("????????????dsl??????=  " + esBuilder.toString());
        return searchRequest;

    }
}
