package com.itheima.ai.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 短期记忆：最近 N 轮原文（Redis List，会话级）
 *
 * 用途：模型做上下文消歧的基础数据
 * TTL：48 小时（超出后自动过期，回到摘要）
 */
@Slf4j
@Component
public class ShortTermMemory {

    private static final String KEY_PREFIX = "ai:memory:session:";
    private static final long TTL_HOURS = 48;
    private static final int MAX_RECENT = 10;   // 只保留最近 10 条（5 轮对话）

    private final StringRedisTemplate redisTemplate;

    public ShortTermMemory(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 添加一条消息（user 或 assistant 通用）
     */
    public void add(String sessionId, String role, String content) {
        String key = buildKey(sessionId);
        try {
            String json = String.format("{\"role\":\"%s\",\"content\":\"%s\"}",
                    role, escape(content));
            redisTemplate.opsForList().rightPush(key, json);

            // 裁剪 + 刷新 TTL
            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size > MAX_RECENT * 2) {
                redisTemplate.opsForList().trim(key, size - MAX_RECENT * 2, -1);
            }
            redisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("ShortTermMemory add 失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 取最近 N 条
     */
    public List<Map<String, String>> getRecent(String sessionId, int count) {
        String key = buildKey(sessionId);
        try {
            List<String> list = redisTemplate.opsForList().range(key, -count, -1);
            if (list == null) return Collections.emptyList();
            List<Map<String, String>> result = new ArrayList<>();
            for (String json : list) {
                Map<String, String> msg = parse(json);
                if (msg != null) result.add(msg);
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * 取全部文本（摘要压缩时用）
     */
    public List<Map<String, String>> getAll(String sessionId) {
        return getRecent(sessionId, MAX_RECENT * 2);
    }

    public void clear(String sessionId) {
        redisTemplate.delete(buildKey(sessionId));
    }

    private String buildKey(String sessionId) {
        return KEY_PREFIX + sessionId;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private Map<String, String> parse(String json) {
        try {
            if (!json.contains("\"role\"")) return null;
            Map<String, String> map = new HashMap<>();
            if (json.contains("\"role\":\"user\"")) map.put("role", "user");
            else if (json.contains("\"role\":\"assistant\"")) map.put("role", "assistant");
            else return null;
            int start = json.indexOf("\"content\":\"") + 11;
            int end = json.lastIndexOf("\"}");
            if (end > start && start > 10) {
                map.put("content", unescape(json.substring(start, end)));
            }
            return map;
        } catch (Exception e) {
            return null;
        }
    }

    private String unescape(String s) {
        return s.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }
}
