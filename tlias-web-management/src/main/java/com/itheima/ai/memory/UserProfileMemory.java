package com.itheima.ai.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.mapper.UserProfileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户画像记忆（MySQL 持久化）
 *
 * 内容：
 *   - favoriteTools：最常用工具的使用次数
 *   - commonIntents：意图分布
 *   - tags：逗号分隔标签（如"就业,考勤,学生管理"）
 *   - lastActiveAt：最近活跃时间
 *
 * 注：目前不依赖 LLM 推断用户画像，仅做结构化统计。
 * 后续可做：根据用户画像走 LLM 生成推荐问题等。
 */
@Slf4j
@Component
public class UserProfileMemory {

    @Autowired
    private UserProfileMapper userProfileMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 记录一次工具调用
     * @param userId 用户 ID
     * @param toolName 工具方法名
     */
    public void recordToolCall(Integer userId, String toolName) {
        if (userId == null || toolName == null) return;
        try {
            Map<String, String> profile = loadOrCreate(userId);
            Map<String, Integer> favTools = parseTools(profile.get("favoriteTools"));
            favTools.merge(toolName, 1, Integer::sum);
            profile.put("favoriteTools", objectMapper.writeValueAsString(favTools));
            profile.put("lastActiveAt", new Date().toString());
            save(userId, profile);
        } catch (Exception e) {
            log.debug("UserProfile 记录工具失败: userId={}, tool={}", userId, toolName);
        }
    }

    /**
     * 记录一次意图
     */
    public void recordIntent(Integer userId, String intent) {
        if (userId == null || intent == null) return;
        try {
            Map<String, String> profile = loadOrCreate(userId);
            Map<String, Integer> intents = parseTools(profile.get("commonIntents"));
            intents.merge(intent, 1, Integer::sum);
            profile.put("commonIntents", objectMapper.writeValueAsString(intents));
            save(userId, profile);
        } catch (Exception e) {
            log.debug("UserProfile 记录意图失败: userId={}, intent={}", userId, intent);
        }
    }

    /**
     * 获取用户画像摘要（用于 Prompt 注入）
     * @return 紧凑 KV 字符串；无画像返回 null
     */
    public String getProfileSummary(Integer userId) {
        try {
            Map<String, String> profile = loadOrCreate(userId);
            String favTools = profile.get("favoriteTools");
            if (favTools == null || favTools.isEmpty() || "{}".equals(favTools)) return null;

            // 取 Top3 工具
            Map<String, Integer> tools = parseTools(favTools);
            String topTools = tools.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(3)
                    .map(e -> e.getKey() + "×" + e.getValue())
                    .collect(Collectors.joining(", "));

            return "用户画像：常用工具=" + topTools;
        } catch (Exception e) {
            return null;
        }
    }

    // ════ 内部 ════

    private Map<String, String> loadOrCreate(Integer userId) {
        try {
            Optional<Map<String, String>> existing = userProfileMapper.findByUserId(userId);
            return existing.orElseGet(this::newProfile);
        } catch (Exception e) {
            return newProfile();
        }
    }

    private Map<String, String> newProfile() {
        Map<String, String> p = new HashMap<>();
        p.put("favoriteTools", "{}");
        p.put("commonIntents", "{}");
        p.put("tags", "");
        p.put("lastActiveAt", new Date().toString());
        return p;
    }

    private void save(Integer userId, Map<String, String> profile) {
        try {
            userProfileMapper.upsert(userId,
                    profile.get("favoriteTools"),
                    profile.get("commonIntents"),
                    profile.get("tags"),
                    profile.get("lastActiveAt"));
        } catch (Exception e) {
            log.debug("UserProfile 保存失败: userId={}", userId);
        }
    }

    private Map<String, Integer> parseTools(String json) {
        if (json == null || json.isEmpty() || "{}".equals(json)) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
