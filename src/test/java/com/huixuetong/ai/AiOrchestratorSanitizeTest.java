package com.huixuetong.ai;

import com.huixuetong.ai.cache.AiAnswerCache;
import com.huixuetong.ai.memory.RedisChatHistoryManager;
import com.huixuetong.ai.memory.SemanticChatMemory;
import com.huixuetong.ai.orchestrator.AiOrchestratorService;
import com.huixuetong.ai.prompt.AiPromptBuilder;
import com.huixuetong.ai.router.Intent;
import com.huixuetong.ai.router.IntentRouter;
import com.huixuetong.ai.advisor.SafeGuardAdvisor;
import com.huixuetong.ai.advisor.TokenTrackingAdvisor;
import com.huixuetong.config.ChatClientConfig;
import com.huixuetong.mapper.ChatRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 验证编排层确实对模型输出做了清洗（不只是清洗器自身可用）。
 * 通过 mock ChatClient 返回泄漏样本，断言 chat() 的返回值被清洗或走兜底。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiOrchestratorSanitizeTest {

    @Mock private ChatClientConfig chatClientConfig;
    @Mock private SafeGuardAdvisor safeGuard;
    @Mock private IntentRouter intentRouter;
    @Mock private AiPromptBuilder promptBuilder;
    @Mock private TokenTrackingAdvisor tokenTracking;
    @Mock private RedisChatHistoryManager historyManager;
    @Mock private SemanticChatMemory semanticChatMemory;
    @Mock private AiAnswerCache aiAnswerCache;
    @Mock private ChatRecordMapper chatRecordMapper;

    @InjectMocks private AiOrchestratorService orchestrator;

    private ChatClient chatClient;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(intentRouter.route(anyString())).thenReturn(Intent.GREETING);
        when(promptBuilder.build(any(), any(), any())).thenReturn("SYS");
    }

    private void stubAnswer(String text) {
        when(chatClientConfig.getChatClient(anyString())).thenReturn(chatClient);
        ChatResponse response = new ChatResponse(List.of(new Generation(new AssistantMessage(text))));

        // 无工具意图：chat() 不会调用 .tools(...)
        when(chatClient.prompt().system(anyString()).user(anyString())
                .call().chatResponse())
                .thenReturn(response);

        // 工具意图：带 List<ToolCallback>
        when(chatClient.prompt().system(anyString()).user(anyString())
                .tools(any(List.class))
                .call().chatResponse())
                .thenReturn(response);
    }

    @Test
    @DisplayName("模型把 DSML 当正文返回时，chat() 应走兜底话术而不是透传")
    void chatReturnsFallbackWhenOnlyToolMarkup() {
        stubAnswer("""
                <｜｜DSML｜｜ calls>
                <｜｜DSML｜｜ invoke name="searchNotice">
                <｜｜DSML｜｜ parameter name="keyword" string="true">缓考</｜｜DSML｜｜ parameter>
                </｜｜DSML｜｜ calls>""");

        String answer = orchestrator.chat("缓考流程是什么", List.of(), "deepseek", 1, "s1");

        assertEquals("抱歉，我这次没能完成查询，请换个问法再试一次。", answer);
    }

    @Test
    @DisplayName("正常回答经 chat() 后内容不变")
    void chatKeepsNormalAnswer() {
        stubAnswer("目前共有 18 名学员。");

        String answer = orchestrator.chat("有多少学员", List.of(), "deepseek", 1, "s1");

        assertEquals("目前共有 18 名学员。", answer);
    }

    @Test
    @DisplayName("工具标记后跟正文时，只删标记保留正文")
    void chatKeepsProseAfterMarkup() {
        stubAnswer("<longcat_tool_call>{\"name\":\"x\"}</longcat_tool_call>缓考需要提交申请表。");

        String answer = orchestrator.chat("缓考流程", List.of(), "deepseek", 1, "s1");

        assertEquals("缓考需要提交申请表。", answer);
    }
}
