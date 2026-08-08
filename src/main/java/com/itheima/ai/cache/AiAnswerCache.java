package com.itheima.ai.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * AI 回答缓存（Cache Aside 模式，v2）
 * - 缓存 key 由调用方构建：userId + sessionId + 最近上下文指纹 + 问题，
 *   同一问题在不同上下文/会话中不会互相污染
 * - 仅缓存无工具意图（寒暄/文本生成）的回答；数据类问答不做缓存
 * - clearByUser 只清理当前用户的缓存，不再全局清空
 * TTL：30分钟
 */
@Slf4j
@Component
public class AiAnswerCache {

    /** v2：key 结构升级，旧版污染缓存自动失效 */
    private static final String CACHE_KEY = "ai:answer:cache:v2";
    private static final long TTL_MINUTES = 30;

    @Autowired
    private StringRedisTemplate redisTemplate;

    public String get(String cacheKey) {
        try {
            Object val = redisTemplate.opsForHash().get(CACHE_KEY, cacheKey);
            if (val != null) {
                log.debug("AI缓存命中: {}", cacheKey.length() > 40 ? cacheKey.substring(0, 40) + "..." : cacheKey);
                return val.toString();
            }
            return null;
        } catch (Exception e) {
            log.debug("AI缓存查询失败: {}", e.getMessage());
            return null;
        }
    }

    public void put(String cacheKey, String answer) {
        try {
            redisTemplate.opsForHash().put(CACHE_KEY, cacheKey, answer);
            redisTemplate.expire(CACHE_KEY, TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("AI缓存写入: {}", cacheKey.length() > 40 ? cacheKey.substring(0, 40) + "..." : cacheKey);
        } catch (Exception e) {
            log.debug("AI缓存写入失败: {}", e.getMessage());
        }
    }


    /**
     * 清空全部 AI 缓存（业务数据变更时调用，兼容旧调用方）
     * 注：数据类问答已不再缓存，此方法仅作保险
     */
    public void clear() {
        try {
            redisTemplate.delete(CACHE_KEY);
            log.info("AI回答缓存已清空");
        } catch (Exception e) {
            log.debug("AI缓存清空失败: {}", e.getMessage());
        }
    }    /**
     * 只清理指定用户（userId 前缀）的缓存，避免影响其他用户
     */
    public void clearByUser(String userIdPrefix) {
        try {
            if (userIdPrefix == null || userIdPrefix.isEmpty()) return;
            Set<Object> fields = redisTemplate.opsForHash().keys(CACHE_KEY);
            if (fields != null) {
                int removed = 0;
                for (Object f : fields) {
                    if (f != null && f.toString().startsWith(userIdPrefix + ":")) {
                        redisTemplate.opsForHash().delete(CACHE_KEY, f);
                        removed++;
                    }
                }
                log.info("AI回答缓存已清理: userIdPrefix={}, removed={}", userIdPrefix, removed);
            }
        } catch (Exception e) {
            log.debug("AI缓存清理失败: {}", e.getMessage());
        }
    }
}