package com.atguigu.service.impl;

import com.atguigu.client.UserFeignClient;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.*;
import com.atguigu.mapper.SeckillProductMapper;
import com.atguigu.result.RetVal;
import com.atguigu.result.RetValCodeEnum;
import com.atguigu.service.SeckillProductService;
import com.atguigu.util.MD5;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author GodWei
 * @since 2022-08-15
 */
@Service
public class SeckillProductServiceImpl extends ServiceImpl<SeckillProductMapper, SeckillProduct> implements SeckillProductService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserFeignClient userFeignClient;

    @Override
    public SeckillProduct getSeckillProductById(Long skuId) {
        // 这里需要从redis中获取数据
        SeckillProduct seckillProduct = (SeckillProduct) redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).get(skuId.toString());
        return seckillProduct;
    }

    //秒杀预下单消费队列的实现方法
    @Override
    public void prepareSecKill(UserSeckillSkuInfo userSeckillSkuInfo) {
        // 获取 skuId userId
        Long skuId = userSeckillSkuInfo.getSkuId();
        String userId = userSeckillSkuInfo.getUserId();
        String state = (String) redisTemplate.opsForValue().get(RedisConst.SECKILL_STATE_PREFIX + skuId.toString());
        //商品已经售罄  //校验秒杀状态位
        if (state.equals(RedisConst.CAN_NOT_SECKILL)) {
            return;
        }
        //判断用户是否下过预购单  库存是否充足 如果有,就减库存; 没有库存, 通知其他节点修改秒杀状态位
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(RedisConst.PREPARE_SECKILL_USERID_SKUID + ":" + userId + ":" + skuId, skuId, RedisConst.PREPARE_SECKILL_LOCK_TIME, TimeUnit.SECONDS);
        if (!flag) {
            return;
        }
        //校验库存       // 减库存
        String redisStockSkuId = (String) redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).rightPop();
        if (StringUtils.isEmpty(redisStockSkuId)) {
            //如果没有库存, 通知其他redis 节点修改秒杀状态位 redis 的publish subscribe
            redisTemplate.convertAndSend(RedisConst.PREPARE_PUB_SUB_SECKILL, skuId + ":" + RedisConst.CAN_NOT_SECKILL);
            return;
        }
        // 生成一个临时订单到redis中, prepare:seckill:userId:order
        PrepareSeckillOrder prepareSeckillOrder = new PrepareSeckillOrder();
        prepareSeckillOrder.setUserId(userId);
        prepareSeckillOrder.setBuyNum(1);
        //生成一个订单码
        prepareSeckillOrder.setPrepareOrderCode(MD5.encrypt(userId + skuId));
        redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).put(userId, prepareSeckillOrder);

        //更新库存信息
        updateSeckillStockCount(skuId);

    }

    // 判断是否具备秒杀资格
    @Override
    public RetVal hasQualified(String userId, Long skuId) {
        //如果预下单里面有用户的信息 就代表具备抢购资格 prepare:seckill:userId:skuId:3:24
        boolean isExist = redisTemplate.hasKey(RedisConst.PREPARE_SECKILL_USERID_SKUID + ":" + userId + ":" + skuId);
        if (isExist) {
            //拿出用户的预购单信息
            PrepareSeckillOrder prepareSeckillOrder = (PrepareSeckillOrder) redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).get(userId);
            if (prepareSeckillOrder != null) {
                return RetVal.build(prepareSeckillOrder, RetValCodeEnum.PREPARE_SECKILL_SUCCESS);
            }
        }
        //如果用户已经购买过该商品
        Integer orderId = (Integer) redisTemplate.boundHashOps(RedisConst.BOUGHT_SECKILL_USER_ORDER).get(userId);
        if (orderId != null) {
            /**
             * 如果订单id不为空 代表该用户购买过该商品 此时页面不能显示为排队中
             * 显示SECKILL_ORDER_SUCCESS 代表抢购成功---页面显示为抢购成功
             */
            return RetVal.build(null, RetValCodeEnum.SECKILL_ORDER_SUCCESS);
        }
        //如果预下单里面没有用户的信息
        return RetVal.build(null, RetValCodeEnum.SECKILL_RUN);
    }

    /**
     * 秒杀页面所需要的数据
     *
     * @param userId
     * @return
     */
    @Override
    public RetVal seckillConfirm(String userId) {
        // 用户收货地址信息 shop-user
        List<UserAddress> userAddressList = userFeignClient.getUserAddressByUserId(userId);
        // 获取用户预购清单
        PrepareSeckillOrder prepareSeckillOrder = (PrepareSeckillOrder) redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).get(userId);
        if (prepareSeckillOrder == null) {
            return RetVal.fail().message("非法请求");
        }
        // 预购单里面的信息转换为订单详情
        SeckillProduct seckillProduct = prepareSeckillOrder.getSeckillProduct();
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setSkuId(seckillProduct.getSkuId());
        orderDetail.setImgUrl(seckillProduct.getSkuDefaultImg());
        orderDetail.setSkuNum(prepareSeckillOrder.getBuyNum() + "");

        //订单的价格 不拿实时价格
        orderDetail.setOrderPrice(seckillProduct.getCostPrice());
        List<OrderDetail> orderDetailList = new ArrayList<>();
        orderDetailList.add(orderDetail);

        //封装成map 返回
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("userAddressList", userAddressList);
        retMap.put("orderDetailList", orderDetailList);
        retMap.put("totalMoney", seckillProduct.getCostPrice());
        return RetVal.ok(retMap);
    }

    private void updateSeckillStockCount(Long skuId) {
        // 剩余库存量  商品数量 redis的list存储, 利用原子队列的原子性, 保证库存不超卖
        Long leftStock = redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).size();
        //更新库存的频次, 自定义一个规则
        if (leftStock % 2 == 0) {
            SeckillProduct seckillProduct = getSeckillProductById(skuId);
            // 锁定库存量 = 商品总数量 - 剩余库存量
            int lockStock = seckillProduct.getNum() - Integer.parseInt(leftStock + "");
            seckillProduct.setStockCount(lockStock);
            //更新redis 是为了前面的方法 给用户看进度
            redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).put(skuId, seckillProduct);
            baseMapper.updateById(seckillProduct);

        }
    }

    // 判断用户是否具备抢购资格  userId skuId
    // 如果预购下单里面有用户的信息,代表具备抢购资格
    // 如果用户已经购买过
    // 如果预下单里面没有用户的信息
}
