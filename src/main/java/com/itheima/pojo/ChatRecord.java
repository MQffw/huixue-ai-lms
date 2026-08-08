package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 对话记录实体
 * 对应表：tlias_ai_chat_record
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRecord {

    private Long id;
    private Integer userId;
    private String sessionId;
    private String userMessage;
    private String aiAnswer;
    private String toolCalls;
    private LocalDateTime createTime;
}
