package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI Token用量统计实体
 * 对应表：tlias_token_usage
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsage {
    private Long id;
    private Integer userId;
    private String modelName;       // deepseek/mimo/longcat
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private String sessionId;
    private LocalDateTime recordTime;
}
