package com.itheima.ai.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * AI 回答缓存（Cache Aside 模式）
 * - 读取：先查缓存，命中则直接返回；未命中则调模型，结果写缓存
 * - 写入：业务数据变更时调用 clear() 清空全部缓存
 *
 * Redis 结构：Hash ai:answer:cache { contextKey → answer }
 * 上下文感知：缓存 key 包含上一轮用户消息，区分同一短消息在不同对话上下文中的含义
 * TTL：30分钟
 */
@Slf4j
@Component
public class AiAnswerCache {

    private static final String CACHE_KEY = "ai:answer:cache";
    private static final long TTL_MINUTES = 30;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 查缓存：完全匹配的问题返回缓存答案
     * @param question 当前问题
     * @param previousContext 上一轮用户消息（用于上下文感知，可为null）
     */
    public String get(String question, String previousContext) {
        try {
            String key = buildKey(question, previousContext);
            Object val = redisTemplate.opsForHash().get(CACHE_KEY, key);
            if (val != null) {
                log.debug("AI缓存命中: {}", key.length() > 30 ? key.substring(0, 30) + "..." : key);
                return val.toString();
            }
            return null;
        } catch (Exception e) {
            log.debug("AI缓存查询失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 写缓存
     * @param question 当前问题
     * @param answer 回答内容
     * @param previousContext 上一轮用户消息（用于上下文感知，可为null）
     */
    public void put(String question, String answer, String previousContext) {
        try {
            String key = buildKey(question, previousContext);
            redisTemplate.opsForHash().put(CACHE_KEY, key, answer);
            redisTemplate.expire(CACHE_KEY, TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("AI缓存写入: {}", key.length() > 30 ? key.substring(0, 30) + "..." : key);
        } catch (Exception e) {
            log.debug("AI缓存写入失败: {}", e.getMessage());
        }
    }

    /**
     * 清空全部AI缓存（业务数据变更时调用）
     */
    public void clear() {
        try {
            redisTemplate.delete(CACHE_KEY);
            log.info("AI回答缓存已清空");
        } catch (Exception e) {
            log.debug("AI缓存清空失败: {}", e.getMessage());
        }
    }

    /**
     * 构建上下文感知的缓存 key
     * 如果存在上一轮消息，将上轮消息作为前缀，避免同一短消息在不同上下文中缓存冲突
     */
    private String buildKey(String question, String previousContext) {
        String base = question.trim().toLowerCase();
        if (previousContext != null && !previousContext.trim().isEmpty()) {
            return previousContext.trim().toLowerCase() + "|" + base;
        }
        return base;
    }
}
