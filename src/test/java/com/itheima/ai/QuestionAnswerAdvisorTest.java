package com.itheima.ai;

import com.itheima.ai.advisor.QuestionAnswerAdvisor;
import com.itheima.mapper.NoticeMapper;
import com.itheima.pojo.Notice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * RAG 知识库检索测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("知识库检索")
class QuestionAnswerAdvisorTest {

    @InjectMocks
    private QuestionAnswerAdvisor advisor;

    @Mock
    private NoticeMapper noticeMapper;

    @Test
    @DisplayName("检索命中制度文档 → 返回上下文")
    void shouldReturnContextOnMatch() {
        Notice n = new Notice();
        n.setTitle("学员退费管理制度");
        n.setContent("一、退费申请条件：学员因个人原因无法继续学习的，可在开课后7天内申请退费。二、退费标准：开课3天内退还90%。");
        n.setType(3);
        when(noticeMapper.findByType(3)).thenReturn(List.of(n));

        String context = advisor.retrieveContext("退费");
        assertTrue(context.contains("退费"));
    }

    @Test
    @DisplayName("检索无匹配 → 返回空字符串")
    void shouldReturnEmptyOnNoMatch() {
        // 与制度相关的查询但 Notice 表中无内容 → 返回空
        when(noticeMapper.findByType(3)).thenReturn(List.of());

        String context = advisor.retrieveContext("退费申请的审批流程是什么");
        assertEquals("", context);
    }

    @Test
    @DisplayName("query为空 → 返回空")
    void shouldHandleEmptyQuery() {
        String context = advisor.retrieveContext("");
        assertEquals("", context);
    }
}
