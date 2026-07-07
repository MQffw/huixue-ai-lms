package com.itheima.ai.rag;

import com.itheima.ai.rag.KnowledgeBaseService;
import com.itheima.pojo.KnowledgeChunk;
import org.springframework.ai.rag.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 知识检索 Advisor（Phase 2 重构版）
 *
 * 替代旧 QuestionAnswerAdvisor：
 *   1. 调用 KnowledgeBaseService.search() 走 FULLTEXT + Re-rank
 *   2. 把 chunk 拼接为上下文注入 Prompt
 *   3. 对得分过低的 query 返回空串（让模型回答"暂无"）
 */
@Component
public class KnowledgeRagAdvisor {

    private static final double MIN_RELEVANCE_SCORE = 1e-4;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    /**
     * 检索知识库内容（用于 AiPromptBuilder 的 KNOWLEDGE_RAG 意图）
     * @param query 用户问题
     * @return 已拼接好的上下文字符串，无命中返回空串
     */
    public String retrieveContext(String query) {
        try {
            String context = knowledgeBaseService.searchToContext(query, 3);
            if (context == null || context.trim().isEmpty()) {
                return "";
            }
            return "【知识库检索结果】\n以下是与用户问题最相关的文档片段（按相关性排序），优先基于这些内容回答用户问题，并注明文档来源：\n\n" + context;
        } catch (Exception e) {
            logDebug("KnowledgeRagAdvisor检索失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 含分数的 chunk 列表（调试用）
     */
    public List<KnowledgeChunk> searchChunks(String query) {
        return knowledgeBaseService.search(query, 3);
    }

    private void logDebug(String msg, String arg) {
        // 生产用 log.debug 避免刷屏
        // log.debug(msg, arg);
    }
}
