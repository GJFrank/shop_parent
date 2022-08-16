package com.atguigu.service.impl;

import com.atguigu.entity.PlatformPropertyKey;
import com.atguigu.entity.PlatformPropertyValue;
import com.atguigu.mapper.PlatformPropertyKeyMapper;
import com.atguigu.mapper.PlatformPropertyValueMapper;
import com.atguigu.service.PlatformPropertyKeyService;
import com.atguigu.service.PlatformPropertyValueService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.ibatis.annotations.Param;
import org.aspectj.weaver.ast.Var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 属性表 服务实现类
 * </p>
 *
 * @author GodWei
 * @since 2022-07-20
 */
@Service
public class PlatformPropertyKeyServiceImpl extends ServiceImpl<PlatformPropertyKeyMapper, PlatformPropertyKey> implements PlatformPropertyKeyService {

    @Autowired
    private PlatformPropertyValueMapper propertyValueMapper;
    @Autowired
    private PlatformPropertyValueService propertyValueService;

    @Override
    public List<PlatformPropertyKey> getPlatformPropertyByCategoryId(Long category1Id, Long category2Id, Long category3Id) {
        return baseMapper.getPlatformPropertyByCategoryId(category1Id, category2Id, category3Id);
    }

    //因为涉及到了两张表的操作, 需要用事务进行管理
    @Transactional
    @Override
    public boolean savePlatformProperty(PlatformPropertyKey platformProperty) {
        //保存平台属性  可以通过传入json的id字段是否为空进行判断
        //a 保存平台key
        if (platformProperty.getId() != null) {
            baseMapper.updateById(platformProperty);
            QueryWrapper<PlatformPropertyValue> wrapper = new QueryWrapper<>();
            wrapper.eq("property_key_id", platformProperty.getId());
            propertyValueService.remove(wrapper);
        } else {
            baseMapper.insert(platformProperty);
        }

        //b 保存平台value
        List<PlatformPropertyValue> propertyValueList = platformProperty.getPropertyValueList();
        for (PlatformPropertyValue propertyValue : propertyValueList) {
            propertyValue.setPropertyKeyId(platformProperty.getId());
        }
        propertyValueService.saveBatch(propertyValueList);

        return true;
    }

    @Override
    public List<PlatformPropertyKey> getPlatformBySkuId(Long skuId) {
        return baseMapper.getPlatformBySkuId(skuId);
    }
}
