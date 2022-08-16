package com.atguigu.mytest;

import com.atguigu.constant.RedisConst;

import com.atguigu.exception.SleepUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/7/28 19:03 周四
 * description:
 */
@Service
public class TestServiceImpl implements TestService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    public ThreadLocal<String> threadLocal = new ThreadLocal<>();

    @Override
    public void testLock() {
        //
        String token = threadLocal.get();
        boolean acquireLock = false;

        //判断是否第一次
        if (acquireLock) {
            token = UUID.randomUUID().toString();
            acquireLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 3, TimeUnit.SECONDS);
        } else {
            acquireLock = true;

        }
        if (acquireLock) {
            doBusiness();
            //定义一个lua脚本
            String luaScript = "if redis.call('get',KEYS[1] == ARGV[1] then return redis.call('del',KEYS[1])else return 0 end";
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(luaScript);
            /*设置执行完脚本返回类型*/
            redisScript.setResultType(Long.class);
            redisTemplate.execute(redisScript, Arrays.asList("lock"), token);
            threadLocal.remove();//擦屁股, 防止内存泄漏
        } else {
            while (true) {
                SleepUtils.sleep(1);

                boolean retryAcquire = redisTemplate.opsForValue().setIfAbsent("lock", token, 3, TimeUnit.SECONDS);
                if (retryAcquire) {
                    threadLocal.set(token);
                    break;
                }
            }
        }
    }

    @Override
    public String readLock() {
        RLock readLock = redissonClient.getReadWriteLock("readwriteLock").readLock();
        readLock.lock(10,TimeUnit.SECONDS);//10秒后,锁自动释放
        String msg = (String) this.redisTemplate.opsForValue().get("msg");
        return  msg;
    }

    @Override
    public String writeLock() {
        RLock writeLock = redissonClient.getReadWriteLock("readwriteLock").writeLock();
        writeLock.lock(10,TimeUnit.SECONDS);
      this.redisTemplate.opsForValue().setIfAbsent("msg", UUID.randomUUID().toString());
        return "成功写入了内容";
    }

    private void doBusiness() {
        String value = (String) redisTemplate.opsForValue().get("num");
        if (StringUtils.isEmpty(value)) {
            redisTemplate.opsForValue().set("num", "1");
        } else {
            //缓存不为空,进行++操作
            int num = Integer.parseInt(value);
            redisTemplate.opsForValue().set("num", String.valueOf(++num));
            System.out.println("进行作业");
        }
    }



}
