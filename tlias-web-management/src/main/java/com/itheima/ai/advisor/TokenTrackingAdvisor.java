package com.itheima.ai.advisor;

import com.itheima.mapper.TokenUsageMapper;
import com.itheima.pojo.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Token 用量追踪器（M6 兼容版）
 * 记录每次对话的 token 消耗到数据库
 */
@Slf4j
@Component
public class TokenTrackingAdvisor {

    @Autowired
    private TokenUsageMapper tokenUsageMapper;

    private static final int DAILY_TOKEN_ALERT_THRESHOLD = 50000;

    /**
     * 记录 token 用量
     */
    public void record(Integer userId, String sessionId, String modelName,
                       long promptTokens, long completionTokens, long totalTokens) {
        try {
            TokenUsage tu = new TokenUsage();
            tu.setUserId(userId != null ? userId : 0);
            tu.setModelName(modelName != null ? modelName : "unknown");
            tu.setPromptTokens((int) promptTokens);
            tu.setCompletionTokens((int) completionTokens);
            tu.setTotalTokens((int) totalTokens);
            tu.setSessionId(sessionId);

            tokenUsageMapper.insert(tu);

            if (totalTokens > DAILY_TOKEN_ALERT_THRESHOLD) {
                log.warn("[TokenTracking] 用户{}当日token消耗已达{}，超过阈值{}",
                        userId, totalTokens, DAILY_TOKEN_ALERT_THRESHOLD);
            }
        } catch (Exception e) {
            log.error("[TokenTracking] 记录token用量失败", e);
        }
    }
}
