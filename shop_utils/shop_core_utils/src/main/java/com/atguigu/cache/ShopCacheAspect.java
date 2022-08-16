package com.atguigu.cache;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.constant.RedisConst;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Reference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.Time;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/7/29 16:21 周五
 * description:
 */
@Component
@Aspect
public class ShopCacheAspect {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @SneakyThrows
    @Around("@annotation(com.atguigu.cache.ShopCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint joinPoint) {
         /*
        1.  获取参数列表
        2.  获取方法上的注解
        3.  获取前缀
        4.  获取目标方法的返回值
         */
        Object object = new Object();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        ShopCache shopCache = signature.getMethod().getAnnotation(ShopCache.class);

        //获取注解上的前缀
        String prefix = shopCache.prefix();
        //方法传入的参数
        Object[] args = joinPoint.getArgs();
        //组成缓存的key
        String key = prefix + Arrays.asList(args).toString();

        //try 防止redis / redisson出问题
        try {
            //从缓存中获取数据
            object = cacheHit(key, signature);

            if (object == null) {
                //从数据库获取
                String lockKey = key + ":lock";
                RLock lock = redissonClient.getLock(lockKey);

                boolean isLocked = lock.tryLock(100, 10, TimeUnit.SECONDS);
                if (isLocked) {
                    try {
                        //表示执行方法体
                        object = joinPoint.proceed(joinPoint.getArgs());
                        //判断是否为空
                        if (object == null) {
                            //若为空, 建个空对象接收
                            Object object1 = new Object();
                            redisTemplate.opsForValue().set(key, JSON.toJSONString(object1), RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                            return object1;
                        } else {
                            //若不为空, 放入缓存
                            redisTemplate.opsForValue().set(key, JSON.toJSONString(object), RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                            return object;
                        }
                    } finally {
                        lock.unlock();
                    }
                } else {
                    //自旋
                    TimeUnit.SECONDS.sleep(1000);
                    return cacheAroundAdvice(joinPoint);
                }
            }else {
                return object;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return joinPoint.proceed(joinPoint.getArgs());
    }

    private Object cacheHit(String key, MethodSignature signature) {
        //通过key 获取缓存的数据
        String strJson = (String) redisTemplate.opsForValue().get(key);
        if (!StringUtils.isEmpty(strJson)) {
            Class returnType = signature.getReturnType();
            return JSON.parseObject(strJson, returnType);
        }
        return null;
    }

}
