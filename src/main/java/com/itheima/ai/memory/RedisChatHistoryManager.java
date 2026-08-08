package com.itheima.ai.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis 聊天历史管理器
 * 存储格式：ai:chat:session:{userId}:{sessionId} → List<JSON消息>
 * TTL：24小时，超过自动过期
 * 上限：40条消息（20轮对话），超过自动裁剪最早消息
 */
@Slf4j
@Component
public class RedisChatHistoryManager {

    private static final String KEY_PREFIX = "ai:chat:session:";
    private static final long TTL_HOURS = 24;
    private static final int MAX_MESSAGES = 40;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取指定会话的历史消息
     */
    public List<Map<String, String>> getHistory(Integer userId, String sessionId) {
        String key = buildKey(userId, sessionId);
        try {
            List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);
            if (jsonList == null || jsonList.isEmpty()) {
                return new ArrayList<>();
            }

            List<Map<String, String>> history = new ArrayList<>();
            for (String json : jsonList) {
                Map<String, String> msg = objectMapper.readValue(json, new TypeReference<>() {});
                history.add(msg);
            }
            return history;
        } catch (Exception e) {
            log.error("读取Redis聊天历史失败: key={}", key, e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存一轮对话（用户消息 + AI回复）
     */
    public void saveRound(Integer userId, String sessionId, String userMessage, String aiAnswer) {
        String key = buildKey(userId, sessionId);
        try {
            // 构造用户消息和AI回复
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);

            Map<String, String> assistantMsg = new HashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", aiAnswer);

            String userJson = objectMapper.writeValueAsString(userMsg);
            String assistantJson = objectMapper.writeValueAsString(assistantMsg);

            // 追加到 Redis List 尾部
            redisTemplate.opsForList().rightPush(key, userJson);
            redisTemplate.opsForList().rightPush(key, assistantJson);

            // 裁剪：只保留最近 MAX_MESSAGES 条
            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size > MAX_MESSAGES) {
                redisTemplate.opsForList().trim(key, size - MAX_MESSAGES, -1);
            }

            // 设置 TTL 24小时
            redisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);

            log.debug("Redis历史已保存: key={}, size={}", key, redisTemplate.opsForList().size(key));
        } catch (JsonProcessingException e) {
            log.error("序列化聊天消息失败", e);
        }
    }

    /**
     * 清除指定会话的历史
     */
    public void clearHistory(Integer userId, String sessionId) {
        String key = buildKey(userId, sessionId);
        redisTemplate.delete(key);
        log.info("已清除会话历史: key={}", key);
    }

    private String buildKey(Integer userId, String sessionId) {
        return KEY_PREFIX + userId + ":" + sessionId;
    }
}
