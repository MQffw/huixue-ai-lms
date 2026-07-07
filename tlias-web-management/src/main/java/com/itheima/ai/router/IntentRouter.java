package com.itheima.ai.router;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * AI 意图路由器 — 规则优先，不调 LLM
 *
 * 优先级从高到低：
 *   1. 安全词直接 reject
 *   2. 统计类关键词 → DATA_STATS
 *   3. 查询类关键词 → DATA_QUERY
 *   4. 制度类关键词 → KNOWLEDGE_RAG
 *   5. 文本生成类关键词 → TEXT_GEN
 *   6. 寒暄/简单消息 → GREETING
 *   7. 兜底 → CHAT
 */
@Slf4j
@Component
public class IntentRouter {

    // ===== 统计类关键词 =====
    private static final List<Pattern> STAT_PATTERNS = List.of(
        Pattern.compile("多少"),
        Pattern.compile("几[个位人]"),
        Pattern.compile("人数"),
        Pattern.compile("分布"),
        Pattern.compile("比例"),
        Pattern.compile("占比"),
        Pattern.compile("百分比"),
        Pattern.compile("占.*多少"),
        Pattern.compile("统计"),
        Pattern.compile("总数"),
        Pattern.compile("一共"),
        Pattern.compile("总计"),
        Pattern.compile("平均"),
        Pattern.compile("排[名序]")  // 成绩排名
    );

    // ===== 查询类关键词 =====
    private static final List<Pattern> QUERY_PATTERNS = List.of(
        Pattern.compile("谁[^的]"),     // "谁请假了" / "谁迟到了"
        Pattern.compile("有没有"),      // "有没有宋江这个人"
        Pattern.compile("是否有"),      // "是否有ID为1的学员"
        Pattern.compile("查找"),
        Pattern.compile("查询"),
        Pattern.compile("搜索"),
        Pattern.compile("列出"),
        Pattern.compile("有哪些"),
        Pattern.compile("哪些[人班级]"),
        Pattern.compile("名单"),
        Pattern.compile("列表"),
        Pattern.compile("最新的?"),
        Pattern.compile("最近"),
        Pattern.compile("未缴"),
        Pattern.compile("欠费"),
        Pattern.compile("异常"),
        Pattern.compile("不正常")
    );

    // ===== 制度/知识类关键词 =====
    private static final List<Pattern> KNOWLEDGE_PATTERNS = List.of(
        Pattern.compile("制度"),
        Pattern.compile("规定"),
        Pattern.compile("规章"),
        Pattern.compile("条例"),
        Pattern.compile("流程"),
        Pattern.compile("请假"),
        Pattern.compile("退费"),
        Pattern.compile("退学"),
        Pattern.compile("休学"),
        Pattern.compile("宿舍"),
        Pattern.compile("违纪处分"),
        Pattern.compile("证书"),
        Pattern.compile("考核"),
        Pattern.compile("管理制"),
        Pattern.compile("公约"),
        Pattern.compile("办法"),
        Pattern.compile("细则")
    );

    // ===== 文本生成类关键词 =====
    private static final List<Pattern> GEN_PATTERNS = List.of(
        Pattern.compile("写"),
        Pattern.compile("撰写"),
        Pattern.compile("起草"),
        Pattern.compile("拟"),
        Pattern.compile("生成"),
        Pattern.compile("总结"),
        Pattern.compile("我[要说想讲]"),
        Pattern.compile("帮我.*(写|做|分析)"),
        Pattern.compile("建议"),
        Pattern.compile("报告"),
        Pattern.compile("翻译"),
        Pattern.compile("解释一下什么是"),
        Pattern.compile("是什么[？?]?$")   // "什么是 Java"
    );

    // ===== 寒暄类关键词 =====
    private static final List<Pattern> GREETING_PATTERNS = List.of(
        Pattern.compile("^(你好|hello|hi|嗨|hey|哈喽)[!！.。]?$"),
        Pattern.compile("谢谢"),
        Pattern.compile("感谢"),
        Pattern.compile("^再见|^bye|^拜拜"),
        Pattern.compile("^好的?[～~]?$"),
        Pattern.compile("^嗯{1,3}[～~]?$")
    );

    /**
     * 路由：返回意图（不会返回 null）
     */
    public Intent route(String question) {
        if (question == null || question.trim().isEmpty()) {
            return Intent.CHAT;
        }
        String q = question.trim();

        // 简单寒暄 / 极短消息（<= 4 字）直接走寒暄
        if (q.length() <= 4 && matchesAny(q, GREETING_PATTERNS)) {
            return Intent.GREETING;
        }

        // 统计类
        if (matchesAny(q, STAT_PATTERNS)) {
            return Intent.DATA_STATS;
        }

        // 查询类（优先于知识库，含"查询请假"、"查找制度"等）
        if (matchesAny(q, QUERY_PATTERNS)) {
            return Intent.DATA_QUERY;
        }

        // 知识库 RAG（纯"请假流程是什么"才走这里）
        if (matchesAny(q, KNOWLEDGE_PATTERNS)) {
            return Intent.KNOWLEDGE_RAG;
        }

        // 文本生成/开放问答
        if (matchesAny(q, GEN_PATTERNS)) {
            return Intent.TEXT_GEN;
        }

        // 兜底
        return Intent.CHAT;
    }

    private boolean matchesAny(String text, List<Pattern> patterns) {
        for (Pattern p : patterns) {
            if (p.matcher(text).find()) return true;
        }
        return false;
    }
}
