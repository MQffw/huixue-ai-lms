package com.itheima.ai.prompt;

import com.itheima.ai.router.Intent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * AI Prompt 动态构造器
 *
 * 按 Intent 类型拼装不同的 Prompt 模块，不再一次性发 2800 字的总 prompt。
 *
 * 拼装规则（每种 Intent 按需组合）：
 * ┌─────────────────┬────────┬──────────┬──────────┬───────────┬────────────┐
 * │ Intent          │ Role+Task│ Tool Cat │ RAG Hint │ Safety    │ Data Dict  │
 * ├─────────────────┼────────┼──────────┼──────────┼───────────┼────────────┤
 * │ DATA_STATS      │    ✓   │   域Tool  │          │     ✓     │     ✓      │
 * │ DATA_QUERY      │    ✓   │   域Tool  │          │     ✓     │     ✓      │
 * │ KNOWLEDGE_RAG   │    ✓   │  Notice  │    ✓     │     ✓     │            │
 * │ TEXT_GEN        │    ✓   │          │          │     ✓     │            │
 * │ CHAT            │    ✓   │  Light   │          │     ✓     │     ✓      │
 * │ GREETING        │    ✓   │          │          │     ✓     │            │
 * └─────────────────┴────────┴──────────┴──────────┴───────────┴────────────┘
 */
@Slf4j
@Component
public class AiPromptBuilder {

    private static final String PROMPT_BASE = "classpath:prompts/";

    // 缓存：classpath 模板
    private String roleAndTask;
    private String safetyRules;
    private String dataDict;
    private String toolCatalogQuery;
    private String toolCatalogStats;
    private String toolCatalogNotice;
    private String ragHint;

    @PostConstruct
    public void init() {
        try {
            roleAndTask   = load("role-and-task.txt");
            safetyRules   = load("safety-rules.txt");
            dataDict      = load("data-dictionary.txt");
            toolCatalogQuery = load("tool-catalog-query.txt");
            toolCatalogStats = load("tool-catalog-stats.txt");
            toolCatalogNotice = load("tool-catalog-notice.txt");
            ragHint       = load("rag-hint.txt");
            log.info("AiPromptBuilder 初始化完成，加载 {} 个 Prompt 模板", 7);
        } catch (Exception e) {
            log.error("AiPromptBuilder 初始化失败", e);
            throw new IllegalStateException("Prompt 模板加载失败", e);
        }
    }

    /**
     * 按 Intent 构造完整 Prompt
     * @param intent 意图类型
     * @param kbContext 知识库上下文（已预检索好，可能为空）
     * @param summary 会话摘要（已预加载好，可能为空）
     * @return 完整 System Prompt 字符串
     */
    public String build(Intent intent, String kbContext, String summary) {
        StringBuilder sb = new StringBuilder(2048);

        // 1. 角色+任务 (所有意图都有)
        sb.append(roleAndTask);

        // 2. 数据字典 (数据类意图才有)
        if (intent == Intent.DATA_STATS || intent == Intent.DATA_QUERY || intent == Intent.CHAT) {
            sb.append("\n\n").append(dataDict);
        }

        // 3. 工具目录 (按意图选择)
        sb.append("\n\n");
        switch (intent) {
            case DATA_STATS -> sb.append(toolCatalogStats);
            case DATA_QUERY -> sb.append(toolCatalogQuery);
            case KNOWLEDGE_RAG -> sb.append(toolCatalogNotice);
            case CHAT -> sb.append(toolCatalogQuery); // 通用查询时给查询工具
            default -> { /* TEXT_GEN / GREETING 不发工具 */ }
        }

        // 4. RAG 提示
        if (intent == Intent.KNOWLEDGE_RAG) {
            sb.append("\n\n").append(ragHint);
            if (kbContext != null && !kbContext.isEmpty()) {
                sb.append("\n\n【知识库检索结果】\n").append(kbContext);
            }
        }

        // 5. 历史摘要 (分段)
        if (summary != null && !summary.isEmpty()) {
            sb.append("\n\n【历史对话摘要】\n").append(summary);
        }

        // 6. 安全规则 (所有意图都有)
        sb.append("\n\n").append(safetyRules);

        return sb.toString();
    }

    private String load(String name) throws IOException {
        ClassPathResource res = new ClassPathResource("prompts/" + name);
        try (var in = res.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
