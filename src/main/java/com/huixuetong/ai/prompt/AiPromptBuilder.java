package com.huixuetong.ai.prompt;

import com.huixuetong.ai.router.Intent;
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
            // KNOWLEDGE_RAG：检索结果已注入，且本意图不挂任何工具；
            // 若这里再列工具目录，模型会尝试发工具调用（DSML），正文被清洗成空
            case KNOWLEDGE_RAG -> { /* 不发工具目录 */ }
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

        // 7. 当前日期 (所有意图都有：模型不知道"今天"，会把最近有数据的日期当成今天)
        sb.append("\n\n【当前日期】").append(currentDateWithWeekday())
          .append("\n凡涉及“今天 / 本周 / 本月 / 最近”的问题，一律以该日期为基准；")
          .append("工具返回的实际统计区间必须在回答中回显，不得用相对词代替具体日期。");

        return sb.toString();
    }

    /** 形如 2026-11-08（星期日） */
    private String currentDateWithWeekday() {
        java.time.LocalDate today = java.time.LocalDate.now();
        String weekday = switch (today.getDayOfWeek()) {
            case MONDAY -> "星期一";
            case TUESDAY -> "星期二";
            case WEDNESDAY -> "星期三";
            case THURSDAY -> "星期四";
            case FRIDAY -> "星期五";
            case SATURDAY -> "星期六";
            case SUNDAY -> "星期日";
        };
        return today + "（" + weekday + "）";
    }

    private String load(String name) throws IOException {
        ClassPathResource res = new ClassPathResource("prompts/" + name);
        try (var in = res.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
