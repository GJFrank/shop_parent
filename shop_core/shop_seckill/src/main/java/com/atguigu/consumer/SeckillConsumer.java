package com.atguigu.consumer;

import com.atguigu.constant.MqConst;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.SeckillProduct;
import com.atguigu.entity.UserSeckillSkuInfo;
import com.atguigu.service.SeckillProductService;
import com.atguigu.utils.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/8/15 9:53 周一
 * description: 秒杀服务消费秒杀消息
 */
@Component
public class SeckillConsumer {
    @Autowired
    private SeckillProductService seckillProductService;
    @Autowired
    private RedisTemplate redisTemplate;

    //1. 接收秒杀商品上架的消息
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.SCAN_SECKILL_QUEUE, durable = "false"),
            exchange = @Exchange(value = MqConst.SCAN_SECKILL_EXCHANGE, durable = "false"),
            key = {MqConst.SCAN_SECKILL_ROUTE_KEY}
    ))
    public void scanSeckill() {
        QueryWrapper<SeckillProduct> wrapper = new QueryWrapper<>();
        //a. 扫描已经通过审核的商品(秒杀商品) status=1 秒杀商品数量>0 当天日期的秒杀商品
        wrapper.eq("status", 1);
        wrapper.gt("num", 0);
        wrapper.ge("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));
        List<SeckillProduct> seckillProductList = seckillProductService.list(wrapper);
        //b. 把秒杀商品放入redis 中 商品信息(hash)  商品数量(list)
        if (!CollectionUtils.isEmpty(seckillProductList)) {
            for (SeckillProduct seckillProduct : seckillProductList) {
                String skuIdStr = seckillProduct.getSkuId().toString();
                // 商品信息 hash
                redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).put(skuIdStr, seckillProduct);
                // 商品数量 redis 的list , 利用redis队列的原子性, 保证库存不超卖,
                for (int i = 0; i < seckillProduct.getNum(); i++) {
                    redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuIdStr).leftPush(skuIdStr);
                }
                //c. 通知其他节点秒杀状态位  生产者
                redisTemplate.convertAndSend(RedisConst.PREPARE_PUB_SUB_SECKILL, skuIdStr + ":" + RedisConst.CAN_SECKILL);
            }
        }
    }

    //2.  消费秒杀预下单里面的消息
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.PREPARE_SECKILL_QUEUE, durable = "false"),
            exchange = @Exchange(value = MqConst.PREPARE_SECKILL_EXCHANGE, durable = "false"),
            key = {MqConst.PREPARE_SECKILL_ROUTE_KEY}
    ))
    public void prepareSeckill(UserSeckillSkuInfo userSeckillSkuInfo) {
        if (userSeckillSkuInfo != null) ;
        //开始处理预下单逻辑
        seckillProductService.prepareSecKill(userSeckillSkuInfo);
    }
}
