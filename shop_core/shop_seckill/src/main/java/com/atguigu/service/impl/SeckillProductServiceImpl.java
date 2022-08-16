package com.atguigu.service.impl;

import com.atguigu.constant.RedisConst;
import com.atguigu.entity.PrepareSeckillOrder;
import com.atguigu.entity.SeckillProduct;
import com.atguigu.entity.UserSeckillSkuInfo;
import com.atguigu.mapper.SeckillProductMapper;
import com.atguigu.service.SeckillProductService;
import com.atguigu.util.MD5;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    @Override

    public SeckillProduct getSeckillProductById(Long skuId) {
        // 这里需要从redis中获取数据
        SeckillProduct seckillProduct = (SeckillProduct) redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).get(skuId.toString());
        return seckillProduct;
    }

    //处理预下单逻辑
    @Override
    public void prepareSecKill(UserSeckillSkuInfo userSeckillSkuInfo) {
        // 获取 skuId userId
        Long skuId = userSeckillSkuInfo.getSkuId();
        String userId = userSeckillSkuInfo.getUserId();
        String state = (String) redisTemplate.opsForValue().get(RedisConst.SECKILL_STATE_PREFIX + skuId.toString());
        //商品已经售罄
        if (state.equals(RedisConst.CAN_NOT_SECKILL)) {
            return;

        }
        //判断用户是否下过预购单  库存是否充足 如果有,就减库存; 没有库存, 通知其他节点修改秒杀状态位
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(RedisConst.PREPARE_SECKILL_USERID_SKUID + ":" + userId + ":" + skuId, skuId, RedisConst.PREPARE_SECKILL_LOCK_TIME, TimeUnit.SECONDS);
        if (!flag) {
            return;
        }
        // 减库存
        String redisStockSkuId = (String) redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).rightPop();
        if (StringUtils.isEmpty(redisStockSkuId)) {
            //如果没有库存, 通知其他redis 节点修改秒杀状态位
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
