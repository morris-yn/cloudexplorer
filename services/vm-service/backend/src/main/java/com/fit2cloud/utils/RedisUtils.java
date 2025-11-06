package com.fit2cloud.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtils {

    private static RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        RedisUtils.redisTemplate = redisTemplate;
    }

    // 静态方法
    public static void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public static Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public static String tryLock(String lockKey, long expireSeconds, long waitTimeoutMs) {
        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        while ((System.currentTimeMillis() - startTime) < waitTimeoutMs) {
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, requestId, expireSeconds, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(success)) {
                // 拿到锁
                return requestId;
            }

            // 拿不到锁就稍等再试
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null; // 超时未获取到锁
    }

    public static boolean unlock(String lockKey, String requestId) {
        String luaScript = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            else
                return 0
            end
            """;

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(luaScript);
        redisScript.setResultType(Long.class);

        Long result = redisTemplate.execute(redisScript, Collections.singletonList(lockKey), requestId);
        return result != null && result == 1;
    }
}
