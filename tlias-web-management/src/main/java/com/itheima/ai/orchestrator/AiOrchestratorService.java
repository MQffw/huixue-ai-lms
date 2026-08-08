package com.itheima.ai.orchestrator;

import com.itheima.ai.router.Intent;
import com.itheima.ai.router.IntentRouter;
import com.itheima.ai.memory.RedisChatHistoryManager;
import com.itheima.ai.memory.SemanticChatMemory;
import com.itheima.ai.advisor.QuestionAnswerAdvisor;
import com.itheima.ai.advisor.SafeGuardAdvisor;
import com.itheima.ai.advisor.SafeGuardAdvisor.PromptInjectionException;
import com.itheima.ai.advisor.SummarizationAdvisor;
import com.itheima.ai.advisor.TokenTrackingAdvisor;
import com.itheima.ai.prompt.AiPromptBuilder;
import com.itheima.ai.tool.*;
import com.itheima.config.ChatClientConfig;
import com.itheima.mapper.ChatRecordMapper;
import com.itheima.pojo.ChatRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AI Orchestrator — 统一编排层
 *
 * 处理链路（一次请求只走其中一条）：
 *   Guard → MemoryLoad → IntentRouter → ToolOrChatOrRAG → AnswerFormat → Trace → Cache → SSE
 *
 * 流式策略（混合模式）：
 *   - 无工具意图（寒暄/文本生成/知识库RAG）：token 级真流式，SSE 逐块推送
 *   - 工具意图（数据查询/统计/通用对话）：先同步执行工具，再分块推送最终答案
 *     （工具结果必须完整返回后才能生成答案，这是业界标准做法）
 */
@Slf4j
@Service
public class AiOrchestratorService {

    // ===== 基础设施 =====
    @Autowired private ChatClientConfig chatClientConfig;
    @Autowired private SafeGuardAdvisor safeGuard;
    @Autowired private IntentRouter intentRouter;
    @Autowired private AiPromptBuilder promptBuilder;
    @Autowired private QuestionAnswerAdvisor questionAnswer;
    @Autowired private SummarizationAdvisor summarization;
    @Autowired private TokenTrackingAdvisor tokenTracking;

    // ===== 数据持久化 =====
    @Autowired private RedisChatHistoryManager historyManager;
    @Autowired private SemanticChatMemory semanticChatMemory;
    @Autowired private com.itheima.ai.cache.AiAnswerCache aiAnswerCache;
    @Autowired private ChatRecordMapper chatRecordMapper;

    // ===== 6 个域 Tool（按 Intent 选择性地注册）=====
    @Autowired private StudentTools studentTools;
    @Autowired private EmployeeTools employeeTools;
    @Autowired private ClazzTools clazzTools;
    @Autowired private CourseTools courseTools;
    @Autowired private AffairsTools affairsTools;
    @Autowired private NoticeTools noticeTools;

    /** 模型降级映射 */
    private static final Map<String, String> FALLBACK_MODEL = Map.of(
        "deepseek", "longcat",
        "longcat", "deepseek",
        "mimo", "deepseek"
    );

    /** 所有模型都不可用时的兜底回复 */
    private static final String[] FALLBACK_MESSAGES = {
        "抱歉，AI助手当前暂时无法提供服务，请稍后再试，或尝试切换其他模型。"
    };

    // ════════════════════════════════════════════
    //  对外入口
    // ════════════════════════════════════════════

    /**
     * 流式对话入口 — 返回真 Spring AI Flux（SSE 级流式）
     *
     * @param message   用户问题
     * @param userId    当前登录用户 ID
     * @param sessionId 会话 ID（来自前端 UUID）
     * @param modelType 模型 deepseek/mimo/longcat
     */
    /**
     * 流式对话入口 — 混合流式
     *
     * 无工具意图（GREETING/TEXT_GEN/KNOWLEDGE_RAG）：token 级真流式，SSE 逐块推送；
     * 工具意图（DATA_STATS/DATA_QUERY/CHAT）：先同步执行工具调用（工具结果必须完整才能生成答案），再分块推送最终答案。
     */
    /**
     * 流式对话入口 — 混合流式
     *
     * 无工具意图（GREETING/TEXT_GEN/KNOWLEDGE_RAG）：token 级真流式，SSE 逐块推送；
     * 工具意图（DATA_STATS/DATA_QUERY/CHAT）：先同步执行工具调用（工具结果必须完整才能生成答案），再分块推送最终答案。
     */
    public Flux<String> stream(String message, Integer userId, String sessionId, String modelType) {
        // 1. 安全拦截（抛异常由 Controller 捕获）
        try {
            safeGuard.check(message);
        } catch (PromptInjectionException e) {
            return Flux.just("[安全提醒] 输入包含不安全的指令，已被安全拦截。");
        }

        // 2. Cache Aside 命中直接返回（缓存键含 userId，防止跨用户串号）
        String effectiveSessionId = sessionId != null ? sessionId : "default";
        String cacheKey = userId != null ? userId + ":" + message : message;
        String cached = aiAnswerCache.get(cacheKey, null);
        if (cached != null) {
            log.info("AI缓存命中直接返回: userId={}, question={}", userId, message);
            return toSimulatedStream(cached);
        }

        // 3. 意图路由
        Intent intent = intentRouter.route(message);
        log.info("意图路由: intent={}, question={}", intent, message);

        // 4. 加载历史
        List<Map<String, String>> history = resolveHistory(userId, effectiveSessionId);
        String summary = semanticChatMemory.getSummary(effectiveSessionId);

        // 5. 知识库 RAG（Intent=KNOWLEDGE_RAG 时预检索）
        String kbContext = "";
        if (intent == Intent.KNOWLEDGE_RAG) {
            kbContext = questionAnswer.retrieveContext(message);
        }

        // 6. 构造 Prompt
        String systemPrompt = promptBuilder.build(intent, kbContext, summary);

        // 6.1 构造带上下文的用户消息（修复：history 加载后从未传入模型）
        String userMessage = buildUserMessage(message, history);

        // 7. 选工具（按 Intent 选对应的 Tool Bean；无工具意图返回空数组）
        Object[] tools = resolveTools(intent);

        // 8. 无工具意图 → token 级真流式
        if (tools.length == 0) {
            return streamAnswer(message, userId, effectiveSessionId, systemPrompt, userMessage,
                    modelType, intent, summary);
        }

        // 9. 工具意图 → 同步执行工具（Spring AI 自动完成 tool_use 循环），再分块推送最终答案
        try {
            ChatClient client = getChatClientWithFallback(modelType);

            // LongCat 已验证支持 Function Calling（返回 finish_reason:"tool_calls"）
            org.springframework.ai.chat.model.ChatResponse chatResponse = client.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .tools(tools)
                .call()
                .chatResponse();

            // 从 ChatResponse 提取最终文本（工具已由 Spring AI 自动执行）
            String finalAnswer = "";
            if (chatResponse != null && chatResponse.getResult() != null
                && chatResponse.getResult().getOutput() != null) {
                finalAnswer = chatResponse.getResult().getOutput().getText();
            }

            finalAnswer = maskSensitiveData(finalAnswer != null ? finalAnswer : "");

            // 持久化 + 缓存
            if (!finalAnswer.isEmpty()) {
                asyncPersist(userId, effectiveSessionId, message, finalAnswer,
                        systemPrompt, modelType, summary, intent);
            }
            log.info("Orchestrator完成(工具意图): userId={}, intent={}, answerLen={}",
                    userId, intent, finalAnswer.length());

            // 工具调用结果整包返回后，按句切块推送（保持 SSE 打字机体验）
            return toSimulatedStream(finalAnswer);

        } catch (Exception e) {
            log.error("Orchestrator 工具调用失败: {}", e.getMessage(), e);
            return toSimulatedStream(FALLBACK_MESSAGES[0]);
        }
    }

    /**
     * token 级真流式（无工具意图）
     *
     * 直接订阅模型的 Flux<String> 输出流，每个 chunk 实时推送；
     * 同时后台累积完整答案，流结束后异步持久化 + 写缓存。
     * 注：数据脱敏在 chunk 级别执行，跨 chunk 的连续敏感串可能漏脱敏（可接受的演示取舍）。
     */
    private Flux<String> streamAnswer(String message, Integer userId, String sessionId,
                                      String systemPrompt, String userMessage, String modelType,
                                      Intent intent, String summary) {
        try {
            ChatClient client = getChatClientWithFallback(modelType);
            StringBuilder collected = new StringBuilder();
            return client.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .stream()
                .content()
                .map(chunk -> maskSensitiveData(chunk != null ? chunk : ""))
                .doOnNext(chunk -> {
                    if (chunk != null) collected.append(chunk);
                })
                .doOnComplete(() -> {
                    String full = collected.toString();
                    if (!full.isEmpty()) {
                        asyncPersist(userId, sessionId, message, full,
                                systemPrompt, modelType, summary, intent);
                    }
                    log.info("Orchestrator流式完成: userId={}, intent={}, answerLen={}",
                            userId, intent, full.length());
                })
                .onErrorResume(e -> {
                    log.error("AI 流式响应异常: {}", e.getMessage(), e);
                    return Flux.just(FALLBACK_MESSAGES[0]);
                });
        } catch (Exception e) {
            log.error("AI 流式初始化失败: {}", e.getMessage(), e);
            return Flux.just(FALLBACK_MESSAGES[0]);
        }
    }


    /**
     * 同步对话入口（测试用）
     */
    public String chat(String message, List<Map<String, String>> history,
                       String modelType, Integer userId, String sessionId) {
        // 安全拦截
        safeGuard.check(message);

        // 意图路由 + 缓存（键含 userId，防止跨用户串号）
        Intent intent = intentRouter.route(message);
        String chatCacheKey = userId != null ? userId + ":" + message : message;
        String cached = aiAnswerCache.get(chatCacheKey, null);
        if (cached != null) return cached;

        // 知识库检索
        String kbContext = (intent == Intent.KNOWLEDGE_RAG) ? questionAnswer.retrieveContext(message) : "";
        String systemPrompt = promptBuilder.build(intent, kbContext, null);

        Object[] tools = resolveTools(intent);
        ChatClient client = getChatClientWithFallback(modelType);

        org.springframework.ai.chat.model.ChatResponse chatResponse = client.prompt()
            .system(systemPrompt)
            .user(message)
            .tools(tools)
            .call()
            .chatResponse();

        String answer = "";
        if (chatResponse != null && chatResponse.getResult() != null
            && chatResponse.getResult().getOutput() != null) {
            answer = maskSensitiveData(chatResponse.getResult().getOutput().getText());
        }

        // 持久化 + 缓存
        if (!answer.isEmpty()) {
            asyncPersist(userId, sessionId != null ? sessionId : "default", message, answer,
                    systemPrompt, modelType, null, intent);
        }

        return answer;
    }

    /**
     * 加载历史
     */
    public List<Map<String, String>> getHistory(Integer userId, String sessionId) {
        if (userId == null || sessionId == null || sessionId.isEmpty()) return List.of();
        return historyManager.getHistory(userId, sessionId);
    }

    // ════════════════════════════════════════════
    //  内部方法
    // ════════════════════════════════════════════

    /**
     * 按 Intent 选 Tool Bean（核心优化：不再发 40 个 tool）
     */
    private Object[] resolveTools(Intent intent) {
        return switch (intent) {
            case DATA_STATS -> new Object[]{ studentTools, employeeTools };
            case DATA_QUERY -> new Object[]{ studentTools, employeeTools, clazzTools, courseTools, affairsTools };
            case KNOWLEDGE_RAG -> new Object[]{}; // RAG 上下文已注入 system prompt，无需工具，可走 token 级流式
            case TEXT_GEN -> new Object[]{};
            case GREETING -> new Object[]{};
            case CHAT -> new Object[]{ studentTools, employeeTools, clazzTools, courseTools, affairsTools, noticeTools };
        };
    }

    /**
     * 带降级的 ChatClient 获取
     */
    private ChatClient getChatClientWithFallback(String modelType) {
        try {
            return chatClientConfig.getChatClient(modelType);
        } catch (Exception e) {
            log.warn("主模型({})创建失败，降级: {}", modelType, e.getMessage());
            String fallbackType = FALLBACK_MODEL.getOrDefault(modelType, "longcat");
            return chatClientConfig.getChatClient(fallbackType);
        }
    }

    /**
     * 构造用户消息：只附带最近 2 轮历史用于消歧，全量历史通过 SummaryMemory 注入
     */
    private String buildUserMessage(String currentQuestion, List<Map<String, String>> history) {
        if (history == null || history.size() <= 1) return currentQuestion;

        // 只取最近 2 轮作为上下文（避免长历史导致模型重复回答旧问题）
        StringBuilder sb = new StringBuilder();
        int fromIndex = Math.max(0, history.size() - 4);
        for (int i = fromIndex; i < history.size(); i++) {
            var msg = history.get(i);
            if ("user".equals(msg.get("role"))) {
                sb.append("用户: ").append(msg.get("content")).append("\n");
            }
        }
        sb.append("\n当前问题: ").append(currentQuestion);
        return sb.toString();
    }

    /**
     * 数据脱敏（委托给 DataMasker 工具类）
     */
    private String maskSensitiveData(String text) {
        return com.itheima.ai.common.DataMasker.mask(text);
    }

    /**
     * 把已有的缓存回答包装成"假流式"Flux（缓存命中时保持 SSE 体验）
     */
    private Flux<String> toSimulatedStream(String answer) {
        return Flux.create(sink -> {
            // 按词组chunk推送（比逐字快，减少 SSE 帧数）
            String[] chunks = answer.split("(?<=(?<=[，。！？,!?\\n]))|(?=\\n)");
            for (String chunk : chunks) {
                if (!chunk.isEmpty()) sink.next(chunk);
            }
            sink.complete();
        });
    }

    private List<Map<String, String>> resolveHistory(Integer userId, String sessionId) {
        if (userId != null && !sessionId.isEmpty()) {
            List<Map<String, String>> redisHistory = historyManager.getHistory(userId, sessionId);
            if (!redisHistory.isEmpty()) return redisHistory;
        }
        return new ArrayList<>();
    }

    // ════════════════════════════════════════════
    //  异步持久化
    // ════════════════════════════════════════════

    private void asyncPersist(Integer userId, String sessionId, String message,
                              String answer, String systemPrompt, String modelType,
                              String summary, Intent intent) {
        try {
            if (answer == null || answer.isEmpty()) return;
            // 避免把占位符和错误兜底消息写入缓存
            if (answer.equals("[streamed]") || answer.startsWith("[错误]")) return;
            if (answer.contains("模型未返回有效响应") || answer.contains("请稍后重试")) return;
            if (answer.contains("[[TOOL]]") || answer.contains("[[END_TOOL]]")) return;

            // 缓存（键含 userId，防止跨用户串号）
            String cacheKey = userId != null ? userId + ":" + message : message;
            aiAnswerCache.put(cacheKey, answer, null);

            // Redis 历史（只用 historyManager，移除 semanticChatMemory 双写）
            if (userId != null && sessionId != null && !sessionId.isEmpty()) {
                historyManager.saveRound(userId, sessionId, message, answer);
            }

            // MySQL 异步落库
            asyncSaveChatRecord(userId, sessionId, message, answer);

            // Token 估算
            int promptChars = systemPrompt.length() + message.length();
            int answerChars = answer.length();
            tokenTracking.record(userId, sessionId, modelType,
                    Math.max(1, (int) (promptChars / 1.3)),
                    Math.max(1, (int) (answerChars / 1.3)),
                    Math.max(2, (int) ((promptChars + answerChars) / 1.3)));

            log.info("Orchestrator持久化完成: userId={}, sessionId={}, intent={}, answerLen={}",
                    userId, sessionId, intent, answerChars);
        } catch (Exception e) {
            log.error("Orchestrator持久化异常", e);
        }
    }

    @Async
    public void asyncSaveChatRecord(Integer userId, String sessionId,
                                    String userMessage, String answer) {
        try {
            if (userId == null) return;
            ChatRecord record = new ChatRecord();
            record.setUserId(userId);
            record.setSessionId(sessionId != null ? sessionId : "default");
            record.setUserMessage(userMessage);
            record.setAiAnswer(answer);
            record.setCreateTime(LocalDateTime.now());
            chatRecordMapper.insert(record);
        } catch (Exception e) {
            log.error("异步持久化对话记录失败: userId={}, sessionId={}", userId, sessionId, e);
        }
    }
}
