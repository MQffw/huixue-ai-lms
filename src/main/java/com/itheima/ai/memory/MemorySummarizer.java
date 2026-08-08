package com.itheima.ai.memory;

import com.itheima.config.ChatClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 对话记忆摘要压缩器（M6 兼容版）
 */
@Slf4j
@Component
public class MemorySummarizer {

    @Autowired
    private ChatClientConfig chatClientConfig;

    @Autowired
    private SemanticChatMemory semanticChatMemory;

    private static final String SUMMARIZE_PROMPT = """
            请将以下对话历史压缩为一段200字以内的摘要，保留关键信息。

            对话历史：
            %s

            请直接返回摘要。
            """;

    /**
     * 压缩对话历史为摘要
     */
    public String summarize(String conversationId, List<Map<String, String>> history) {
        if (history == null || history.isEmpty()) return "";

        StringBuilder dialogText = new StringBuilder();
        for (Map<String, String> msg : history) {
            String role = "user".equals(msg.get("role")) ? "用户" : "助手";
            String content = msg.get("content");
            if (content != null && content.length() > 200) {
                content = content.substring(0, 200) + "...";
            }
            dialogText.append(role).append(": ").append(content).append("\n");
        }

        String prompt = String.format(SUMMARIZE_PROMPT, dialogText.toString());

        try {
            ChatClient chatClient = chatClientConfig.getChatClient("longcat");
            String summary = chatClient.prompt().user(prompt).call().content();

            if (summary != null && summary.length() > 300) {
                summary = summary.substring(0, 300);
            }

            semanticChatMemory.updateSummary(conversationId, summary);
            log.info("对话摘要压缩完成: conversationId={}, summaryLen={}",
                    conversationId, summary != null ? summary.length() : 0);

            return summary != null ? summary : "";
        } catch (Exception e) {
            log.error("对话摘要压缩失败: conversationId={}", conversationId, e);
            return "";
        }
    }
}
