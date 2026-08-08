package com.itheima.ai.router;

/**
 * AI 意图枚举
 *
 * 由 IntentRouter 规则层决定，Intent 决定：
 * 1. 注册哪几个 Tool（选 Bean）
 * 2. 是否要 RAG
 * 3. 是否做 SQL 兜底
 */
public enum Intent {
    /**
     * 数据统计（多少/比例/分布）→ 仅注入 Student/Employee 的 count* 工具
     */
    DATA_STATS,

    /**
     * 数据查询（谁/查找/列表）→ 注入全部查询类 Tool
     */
    DATA_QUERY,

    /**
     * 制度/规范/流程类 → 知识库 RAG + Notice 工具
     */
    KNOWLEDGE_RAG,

    /**
     * 文本生成/开放问答 → 纯 LLM，不注入 Tool
     */
    TEXT_GEN,

    /**
     * 通用对话（兜底）→ 注入查询+Notice 工具
     */
    CHAT,

    /**
     * 打招呼/简单寒暄 → 纯 LLM
     */
    GREETING
}
