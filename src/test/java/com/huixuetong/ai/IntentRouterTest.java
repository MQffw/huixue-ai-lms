package com.huixuetong.ai;

import com.huixuetong.ai.router.Intent;
import com.huixuetong.ai.router.IntentRouter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 意图路由回归：文档/制度类问题不能被"有哪些/多少"抢走成数据查询。
 */
class IntentRouterTest {

    private final IntentRouter router = new IntentRouter();

    @Test
    @DisplayName("论文目录类问题走知识库")
    void documentQuestionGoesToRag() {
        assertEquals(Intent.KNOWLEDGE_RAG, router.route("论文撰写的目录有哪些部分"));
        assertEquals(Intent.KNOWLEDGE_RAG, router.route("毕业论文的格式要求是什么"));
        assertEquals(Intent.KNOWLEDGE_RAG, router.route("论文大纲有几章"));
    }

    @Test
    @DisplayName("带业务实体的查询仍走数据查询")
    void entityQueryStaysDataQuery() {
        assertEquals(Intent.DATA_QUERY, router.route("有哪些学员"));
        assertEquals(Intent.DATA_QUERY, router.route("查询请假记录"));
    }

    @Test
    @DisplayName("统计类不受影响")
    void statsStillStats() {
        assertEquals(Intent.DATA_STATS, router.route("各学历学生人数统计"));
        assertEquals(Intent.DATA_STATS, router.route("今天有多少学员考勤异常"));
    }

    @Test
    @DisplayName("纯制度问题走知识库")
    void policyGoesToRag() {
        assertEquals(Intent.KNOWLEDGE_RAG, router.route("请假流程是什么"));
        assertEquals(Intent.KNOWLEDGE_RAG, router.route("学生请假流程"));
        assertEquals(Intent.KNOWLEDGE_RAG, router.route("查询学生请假制度"));
    }

    @Test
    @DisplayName("实体 + 数据动作仍走数据查询/统计")
    void entityActionsStayData() {
        assertEquals(Intent.DATA_QUERY, router.route("查询违纪记录"));
        assertEquals(Intent.DATA_QUERY, router.route("列出欠费学员"));
        assertEquals(Intent.DATA_STATS, router.route("有多少学员请假"));
    }
}
