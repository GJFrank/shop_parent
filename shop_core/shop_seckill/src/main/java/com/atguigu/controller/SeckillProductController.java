package com.atguigu.controller;


import com.atguigu.client.OrderFeignClient;
import com.atguigu.constant.MqConst;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.OrderInfo;
import com.atguigu.entity.PrepareSeckillOrder;
import com.atguigu.entity.SeckillProduct;
import com.atguigu.entity.UserSeckillSkuInfo;
import com.atguigu.result.RetVal;
import com.atguigu.result.RetValCodeEnum;
import com.atguigu.service.SeckillProductService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.util.MD5;
import com.atguigu.utils.DateUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author GodWei
 * @since 2022-08-15
 */
@RestController
@RequestMapping("/seckill")
public class SeckillProductController {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SeckillProductService seckillProductService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private OrderFeignClient orderFeignClient;

    //1. 秒杀商品列表显示
    @GetMapping("queryAllSecKillProduct")
    public List<SeckillProduct> queryAllSecKillProduct() {
        List<SeckillProduct> seckillProductList = redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).values();
        return seckillProductList;
    }

    //2. 单个秒杀商品详情
    @GetMapping("querySecKillProductById/{skuId}")
    public SeckillProduct querySecKillProductById(@PathVariable Long skuId) {

        SeckillProduct seckillProduct = seckillProductService.getSeckillProductById(skuId);
        return seckillProduct;
    }

    //3. 生成抢购码 http://api.gmall.com/seckill/generateSeckillCode/24
    @GetMapping("/generateSeckillCode/{skuId}")
    public RetVal generateSeckillCode(@PathVariable Long skuId, HttpServletRequest request) {
        // 1. 判断用户是否登录
        String userId = AuthContextHolder.getUserId(request);
        // userId 非空表示用户已登录
        if (!StringUtils.isEmpty(userId)) {
            //2. 从缓存中拿到秒杀商品信息
            SeckillProduct seckillProduct = seckillProductService.getSeckillProductById(skuId);
            //3. 判断当前时间是否在秒杀范围内  //亦可判断秒杀状态位
            Date currentDate = new Date();
            //前者<=后者 true
            if (DateUtil.dateCompare(seckillProduct.getStartTime(), currentDate) && DateUtil.dateCompare(currentDate, seckillProduct.getEndTime())) {
                //4.利用md5对用户Id进行加密, 生成一个抢购码
                String seckillCode = MD5.encrypt(userId);
                return RetVal.ok(seckillCode);
            }
        }
        //未登录 失败并提示 用户需要登录
        return RetVal.fail().message("获取抢购码失败, 请先登录!");
    }

    // 4. 秒杀预下单  seckill-queue.html?skuId=33&seckillCode=eccbc87e4b5ce2fe28308fd9f2a7baf3
    @PostMapping("/prepareSeckill/{skuId}")
    public RetVal prepareSeckill(@PathVariable Long skuId, String seckillCode, HttpServletRequest request) {
        //a. 判断请购码是否正确  承接第三步, 需要获取抢购码
        String userId = AuthContextHolder.getUserId(request);
        if (!MD5.encrypt(userId).equals(seckillCode)) {
            // 抢购码不合法 报异常
            return RetVal.build(null, RetValCodeEnum.SECKILL_ILLEGAL);
        }
        //b. 判断秒杀商品是否可以进行秒杀, 状态位为1 可以秒杀  // 为空, 报错; 正常, 生成一个预下单
        String state = (String) redisTemplate.boundValueOps(RedisConst.SECKILL_STATE_PREFIX + skuId.toString()).get();
        if (StringUtils.isEmpty(state)) {
            // 如果秒杀状态为不存在, 报异常
            return RetVal.build(null, RetValCodeEnum.SECKILL_ILLEGAL);
        }
        // 如果可以秒杀, 生成预下单
        if (state.equals(RedisConst.CAN_SECKILL)) {
            UserSeckillSkuInfo userSeckillSkuInfo = new UserSeckillSkuInfo();
            userSeckillSkuInfo.setSkuId(skuId);
            userSeckillSkuInfo.setUserId(userId);
            rabbitTemplate.convertAndSend(MqConst.PREPARE_SECKILL_EXCHANGE, MqConst.PREPARE_SECKILL_ROUTE_KEY, userSeckillSkuInfo);


        } else {
            // 秒杀商品已售罄
            return RetVal.build(null, RetValCodeEnum.SECKILL_FINISH);
        }
        return RetVal.ok();
    }

    //5. 判断是否具备抢购资格 http://api.gmall.com/seckill/hasQualified/24
    @GetMapping("hasQualified/{skuId}")
    public RetVal hasQualified(@PathVariable Long skuId, HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        return seckillProductService.hasQualified(userId, skuId);
    }

    //6. 返回秒杀页面需要的数据
    @GetMapping("seckillConfirm")
    public RetVal seckillConfirm(HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        return seckillProductService.seckillConfirm(userId);
    }

    //7. 提交秒杀订单信息
    @PostMapping("submitSecKillOrder")
    public RetVal submitSecKillOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        // 判断用户是否有预购单
        PrepareSeckillOrder prepareSeckillOrder = (PrepareSeckillOrder) redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).get(userId);
        if (prepareSeckillOrder == null) {
            return RetVal.fail().message("非法请求");
        }
        // 通过远程调用order微服务进行下单
        Long orderId = orderFeignClient.saveOrderAndDetail(orderInfo);
        if (orderId == null) {
            return RetVal.fail().message("下单失败!");
        }

        // 删除预购单信息
        redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).delete(userId);
        // 在redis中把用户购买的商品信息放里面,  用于判断该用户是否购买过商品
        redisTemplate.boundHashOps(RedisConst.BOUGHT_SECKILL_USER_ORDER).put(userId, orderId);
        return RetVal.ok(orderId);

    }


}

