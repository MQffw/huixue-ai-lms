package com.huixuetong.ai;

import com.huixuetong.ai.prompt.AiPromptBuilder;
import com.huixuetong.ai.router.Intent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prompt 构造测试：重点验证"当前日期"注入，治模型把最近有数据的日期当今天。
 */
class AiPromptBuilderTest {

    private static AiPromptBuilder builder;

    @BeforeAll
    static void setUp() {
        builder = new AiPromptBuilder();
        builder.init();
    }

    @Test
    @DisplayName("所有意图的 Prompt 都应包含真实当前日期与星期")
    void promptContainsRealDate() {
        String prompt = builder.build(Intent.CHAT, null, null);

        assertTrue(prompt.contains("【当前日期】"), "缺少日期段");
        assertTrue(prompt.contains(LocalDate.now().toString()),
                "缺少真实日期 " + LocalDate.now());
        assertTrue(prompt.contains("星期"), "缺少星期");
        assertTrue(prompt.contains("不得用相对词代替具体日期"), "缺少日期回显约束");
    }

    @Test
    @DisplayName("知识库上下文应被拼进 Prompt")
    void knowledgeContextIsInjected() {
        String prompt = builder.build(Intent.KNOWLEDGE_RAG, "缓考须在考前 3 天申请。", null);

        assertTrue(prompt.contains("缓考须在考前 3 天申请。"));
        assertTrue(prompt.contains("【知识库检索结果】"));
    }

    @Test
    @DisplayName("空知识库上下文时不应注入检索结果段")
    void emptyContextDoesNotLeakHeader() {
        String prompt = builder.build(Intent.KNOWLEDGE_RAG, "", null);

        // rag-hint 模板本身提到过这个标题，这里只校验"注入段"（前后带换行的独立段落）不存在
        assertFalse(prompt.contains("\n\n【知识库检索结果】\n"));
    }
}
