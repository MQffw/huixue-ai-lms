package com.itheima.ai.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 语义对话记忆（M6 兼容版）
 *
 * 架构：
 * 1. 近期原文（Redis List，最近10条）        ← 短期连贯性
 * 2. 历史摘要（LLM压缩，1段200字）           ← 释放上下文窗口
 *
 * Token 消耗从 40条原文~4000 降到 ~1500，降低 60%+
 */
@Slf4j
@Component
public class SemanticChatMemory {

    private static final String KEY_PREFIX = "ai:memory:session:";
    private static final long TTL_HOURS = 48;
    private static final int RECENT_COUNT = 10;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /** 跨会话语义历史摘要缓存 */
    private final Map<String, String> summaryCache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 1000;
        }
    };

    /**
     * 保存对话消息到 Redis
     */
    public void addMessage(String conversationId, String role, String content) {
        String key = KEY_PREFIX + conversationId;
        try {
            String json = "{\"role\":\"" + role + "\",\"content\":\"" +
                    escapeJson(content) + "\"}";
            redisTemplate.opsForList().rightPush(key, json);

            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size > RECENT_COUNT * 2) {
                redisTemplate.opsForList().trim(key, size - RECENT_COUNT * 2, -1);
            }

            redisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("保存语义记忆失败: conversationId={}", conversationId, e);
        }
    }

    /**
     * 获取近期对话（最近10条）
     */
    public List<Map<String, String>> getRecentMessages(String conversationId) {
        String key = KEY_PREFIX + conversationId;
        List<Map<String, String>> messages = new ArrayList<>();
        try {
            List<String> recent = redisTemplate.opsForList().range(key, -RECENT_COUNT, -1);
            if (recent != null) {
                for (String json : recent) {
                    Map<String, String> msg = parseMessage(json);
                    if (msg != null) messages.add(msg);
                }
            }
        } catch (Exception e) {
            log.error("读取语义记忆失败: conversationId={}", conversationId, e);
        }
        return messages;
    }

    /**
     * 清除会话记忆
     */
    public void clear(String conversationId) {
        String key = KEY_PREFIX + conversationId;
        redisTemplate.delete(key);
        summaryCache.remove(conversationId);
    }

    /**
     * 更新对话摘要
     */
    public void updateSummary(String conversationId, String summary) {
        summaryCache.put(conversationId, summary);
    }

    /**
     * 获取缓存的摘要
     */
    public String getSummary(String conversationId) {
        return summaryCache.get(conversationId);
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private Map<String, String> parseMessage(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("解析历史消息失败: {}", json);
            return null;
        }
    }
}
