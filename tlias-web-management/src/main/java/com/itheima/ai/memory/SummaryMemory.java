package com.itheima.ai.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 摘要记忆：LLM 压缩的历史对话摘要（Redis String）
 *
 * 用途：当短期记忆超过 10 轮时，用 LLM 把早期对话压缩为 200 字摘要
 * TTL：与短期记忆同步（48 小时）；重启不丢失（Redis 持久化）
 *
 * 简化版（不调 LLM，用文本摘要近似）：
 *   - 取早期对话的首句 + 关键信息（含关键词的句子）
 *   - 拼装为不超过 300 字的摘要文本
 * 后续可用 LLM 调用替换此近似摘要以获得更好效果。
 */
@Slf4j
@Component
public class SummaryMemory {

    private static final String KEY_PREFIX = "ai:summary:session:";
    private static final long TTL_HOURS = 48;
    private static final int SUMMARY_MAX_CHARS = 300;

    private final StringRedisTemplate redisTemplate;

    public SummaryMemory(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取会话摘要（没有则返回 null）
     */
    public String get(String sessionId) {
        try {
            Object val = redisTemplate.opsForValue().get(KEY_PREFIX + sessionId);
            return val != null ? val.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 保存摘要（Redis String，带 TTL）
     */
    public void save(String sessionId, String summary) {
        try {
            String truncated = summary.length() > SUMMARY_MAX_CHARS
                    ? summary.substring(0, SUMMARY_MAX_CHARS)
                    : summary;
            redisTemplate.opsForValue().set(KEY_PREFIX + sessionId, truncated, TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("SummaryMemory save 失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 当短期记忆超过阈值时，基于早期对话生成摘要（简化版：首句提取）
     * 返回 true 表示已触发压缩
     */
    public boolean compressIfNeeded(String sessionId, List<Map<String, String>> allRecent, int threshold) {
        if (allRecent.size() <= threshold) return false;
        // 取前半部分为待压缩内容（超过阈值的早期条目）
        int compressEnd = allRecent.size() - threshold;
        List<Map<String, String>> toCompress = allRecent.subList(0, Math.max(1, compressEnd));

        StringBuilder sb = new StringBuilder();
        for (Map<String, String> msg : toCompress) {
            String role = "user".equals(msg.get("role")) ? "用户" : "助手";
            String content = msg.get("content");
            if (content != null) {
                // 截断到 80 字/句
                sb.append(role).append(": ");
                sb.append(content.length() > 80 ? content.substring(0, 80) + "..." : content);
                sb.append("\n");
            }
        }
        save(sessionId, sb.toString().trim());
        return true;
    }

    public void clear(String sessionId) {
        redisTemplate.delete(KEY_PREFIX + sessionId);
    }
}
