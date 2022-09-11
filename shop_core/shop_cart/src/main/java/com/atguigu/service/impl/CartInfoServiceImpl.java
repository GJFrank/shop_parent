package com.atguigu.service.impl;

import com.atguigu.client.ProductFeignClient;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.CartInfo;
import com.atguigu.entity.SkuInfo;
import com.atguigu.mapper.CartInfoMapper;
import com.atguigu.service.CartInfoService;
import com.atguigu.util.AuthContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 购物车表 用户登录系统时更新冗余 服务实现类
 * </p>
 *
 * @author GodWei
 * @since 2022-08-08
 */
@Service
public class CartInfoServiceImpl extends ServiceImpl<CartInfoMapper, CartInfo> implements CartInfoService {
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void addToCart(String oneOfUserId, Long skuId, Integer skuNum) {
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", oneOfUserId);
        wrapper.eq("sku_id", skuId);
        CartInfo existCartInfo = baseMapper.selectOne(wrapper);
        //如果已存在
        if (existCartInfo != null) {
            //更新传递过来的数量 价格
            existCartInfo.setSkuNum(existCartInfo.getSkuNum() + skuNum);
            existCartInfo.setRealTimePrice(productFeignClient.getSkuPrice(skuId));
            //更新对象 updateById
            baseMapper.updateById(existCartInfo);
        } else {
            //如果不存在 插入一条新数据
            existCartInfo = new CartInfo();
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            existCartInfo.setUserId(oneOfUserId);
            existCartInfo.setSkuId(skuInfo.getId());
            existCartInfo.setCartPrice(skuInfo.getPrice());
            existCartInfo.setSkuNum(skuNum);
            existCartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            existCartInfo.setSkuName(skuInfo.getSkuName());
            //默认设置 勾选商品
            existCartInfo.setIsChecked(1);
            existCartInfo.setRealTimePrice(productFeignClient.getSkuPrice(skuId));
            //插入数据库 insert
            baseMapper.insert(existCartInfo);

        }
        String userKey = getUserCartKey(oneOfUserId);
        //todo 是用了redis的 hash 吗
        redisTemplate.boundHashOps(userKey).put(skuId.toString(), existCartInfo);
    }

    @Override
    public List<CartInfo> getCartList(HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        String userTempId = AuthContextHolder.getUserTempId(request);
        List<CartInfo> cartInfoList = new ArrayList<>();
        //1. 未登录
        if (StringUtils.isEmpty(userId) || !StringUtils.isEmpty(userTempId)) {
            cartInfoList = queryFromDbToRedis(userTempId);
        }
        //2. 已登录
        if (!StringUtils.isEmpty(userId)) {
            //查询未登录的购物车信息
            List<CartInfo> noLoginCartInfoList = queryFromDbToRedis(userTempId);
            if (!CollectionUtils.isEmpty(noLoginCartInfoList)) {
                //合并已登录和未登录的购物项
                mergeCartInfoList(userId, userTempId);
                //合并之后删除临时用户的购物项
                cartInfoList = deleteNoLoginDataAndReload(userId, userTempId);
            } else {
                cartInfoList = queryFromDbToRedis(userId);
            }

        }
        return cartInfoList;
    }

    // 更新选中状态
    @Override
    public void checkCart(String oneOfUserId, Long skuId, Integer isChecked) {
        //理论上需要先修改数据库,再修改redis.  这里使用先redis再mysql的顺序是为了学习
        //a. 从redis获取数据
        String userCartKey = getUserCartKey(oneOfUserId);
        BoundHashOperations boundHashOps = redisTemplate.boundHashOps(userCartKey);

        //b. 根据skuId从Hash里面拿到购物车的信息
        if (boundHashOps.hasKey(skuId.toString())) {
            CartInfo redisCartInfo = (CartInfo) boundHashOps.get(skuId.toString());
            //更新选中状态
            redisCartInfo.setIsChecked(isChecked);
            //c 更新到redis里面
            boundHashOps.put(skuId.toString(), redisCartInfo);
            //d. 设置过期时间
            setCartKeyExpire(userCartKey);
        }
        //e. 更新数据库
        checkCartInfoFromDb(oneOfUserId, skuId, isChecked);
    }

    @Override
    public void deleteCart(String oneOfUserId, Long skuId) {
        //删除redis里面的
        String userCartKey = getUserCartKey(oneOfUserId);
        BoundHashOperations boundHashOps = redisTemplate.boundHashOps(userCartKey);
        if (boundHashOps.hasKey(skuId.toString())) {
            boundHashOps.delete(skuId.toString());
        }
        //删除数据库
        deleteFromDb(oneOfUserId, skuId);
    }

    @Override
    public List<CartInfo> getSelectedCartInfo(String userId) {
        //从redis拿
        List<CartInfo> selectedCartInfoList = new ArrayList<>();
        String userCartKey = getUserCartKey(userId);
        List<CartInfo> redisCartInfoList = redisTemplate.opsForHash().values(userCartKey);
        for (CartInfo cartInfo : redisCartInfoList) {
            if (cartInfo.getIsChecked() == 1) {
                selectedCartInfoList.add(cartInfo);
            }
        }
        return selectedCartInfoList;
    }

    private void deleteFromDb(String oneOfUserId, Long skuId) {
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", oneOfUserId);
        wrapper.eq("sku_id", skuId);
        baseMapper.delete(wrapper);
    }

    //更新数据库里的cartInfo的isChecked
    private void checkCartInfoFromDb(String oneOfUserId, Long skuId, Integer isChecked) {
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", oneOfUserId);
        wrapper.eq("sku_id", skuId);
        baseMapper.update(cartInfo, wrapper);
    }

    private List<CartInfo> deleteNoLoginDataAndReload(String userId, String userTempId) {
        //1. 删除db数据库里面的tempUser信息
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userTempId);  //基本没需求, 在合并哪一步就没有userTempId了? todo
        baseMapper.delete(wrapper);
        //2. 删除redis里面的tempUser 信息
        String userCartKey = getUserCartKey(userId);
        String userTempCartKey = getUserCartKey(userTempId);
        redisTemplate.delete(userCartKey);
        redisTemplate.delete(userTempCartKey);
        //3. 重新加载信息到redis中
        return queryFromDbToRedis(userId);
    }

    private void mergeCartInfoList(String userId, String userTempId) {
        // 第一种方案, 因为用了两个for循环, 性能不佳, 不推荐
//        for (CartInfo noLoginCartInfo : noLoginCartInfoList) {
//            for (CartInfo existCartInfo : existCartInfoList) {
//                if (noLoginCartInfo.getSkuId() == existCartInfo.getSkuId()) {
//                    existCartInfo.setSkuNum(existCartInfo.getSkuNum() + noLoginCartInfo.getSkuNum());
//                    //当未登录的勾选状态为0 , 合并之后勾选状态为1
//                    if (noLoginCartInfo.getIsChecked() == 0) {
//                        existCartInfo.setIsChecked(1);
//                    }
//                    baseMapper.updateById(existCartInfo);
//                } else {
//                    noLoginCartInfo.setUserId(existCartInfo.getUserId());
//                    baseMapper.updateById(noLoginCartInfo);
//                }
//            }
//        }
        //未登录的购物项
        List<CartInfo> noLoginCartInfoList = queryFromDbToRedis(userTempId);
        //已登录的购物项
        List<CartInfo> loginCartInfoList = queryFromDbToRedis(userId);

        //把已登录的转换为map
        Map<Long, CartInfo> loginCartInfoMap = loginCartInfoList.stream().collect(Collectors.toMap(CartInfo::getSkuId, cartInfo -> cartInfo));
        for (CartInfo noLoginCartInfo : noLoginCartInfoList) {
            //代表未登录和已登录都添加了该购物项
            if (loginCartInfoMap.containsKey(noLoginCartInfo.getSkuId())) {
                CartInfo loginCartInfo = loginCartInfoMap.get(noLoginCartInfo.getSkuId());
                //把未登录和已登录数量相加
                loginCartInfo.setSkuNum(loginCartInfo.getSkuNum() + noLoginCartInfo.getSkuNum());
                //当未登录的时候该商品未勾选 合并之后需要勾选
                if (noLoginCartInfo.getIsChecked() == 0) {
                    loginCartInfo.setIsChecked(1);
                }
                //更新数据库
                baseMapper.updateById(loginCartInfo);
            } else {
                //如果已登录没有该购物项
                noLoginCartInfo.setUserId(userId);
                baseMapper.updateById(noLoginCartInfo);
            }
        }

    }

    private List<CartInfo> queryFromDbToRedis(String oneOfUserId) {
        String userCartKey = getUserCartKey(oneOfUserId);
        List<CartInfo> cartInfoList = redisTemplate.boundHashOps(userCartKey).values();

        //如果缓存里没有数据,从数据库拿 放入缓存, key,cartInfo
        if (CollectionUtils.isEmpty(cartInfoList)) {
            QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", oneOfUserId);
            cartInfoList = baseMapper.selectList(wrapper);

            Map<String, CartInfo> cartInfoMap = new HashMap<>();
            for (CartInfo cartInfo : cartInfoList) {
                cartInfoMap.put(cartInfo.getSkuId().toString(), cartInfo);
            }
            redisTemplate.boundHashOps(userCartKey).putAll(cartInfoMap);
            //设置redis的过期时间
            setCartKeyExpire(userCartKey);
        }

        return cartInfoList;
    }

    private void setCartKeyExpire(String userCartKey) {
        redisTemplate.expire(userCartKey, RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }

    private String getUserCartKey(String userId) {
        String userCartKey = RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
        return userCartKey;
    }
}
