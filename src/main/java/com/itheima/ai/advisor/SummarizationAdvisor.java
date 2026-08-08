package com.itheima.ai.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 对话摘要压缩检查器（M6 兼容版）
 * 检查对话轮次，超过阈值时标记需要摘要压缩
 */
@Slf4j
@Component
public class SummarizationAdvisor {

    private static final int SUMMARY_THRESHOLD_ROUNDS = 20;

    /**
     * 检查是否需要摘要压缩
     * @param roundCount 当前对话轮次
     * @return true 表示需要摘要压缩
     */
    public boolean needsSummarization(int roundCount) {
        boolean needs = roundCount > SUMMARY_THRESHOLD_ROUNDS;
        if (needs) {
            log.info("[Summarization] 对话已超过{}轮（当前{}轮），需要摘要压缩", SUMMARY_THRESHOLD_ROUNDS, roundCount);
        }
        return needs;
    }
}
