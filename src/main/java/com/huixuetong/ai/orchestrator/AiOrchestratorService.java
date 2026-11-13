package com.huixuetong.ai.orchestrator;

import com.huixuetong.ai.router.Intent;
import com.huixuetong.ai.router.IntentRouter;
import com.huixuetong.ai.memory.RedisChatHistoryManager;
import com.huixuetong.ai.memory.SemanticChatMemory;
import com.huixuetong.ai.advisor.QuestionAnswerAdvisor;
import com.huixuetong.ai.advisor.SafeGuardAdvisor;
import com.huixuetong.ai.advisor.SafeGuardAdvisor.PromptInjectionException;
import com.huixuetong.ai.advisor.SummarizationAdvisor;
import com.huixuetong.ai.advisor.TokenTrackingAdvisor;
import com.huixuetong.ai.common.AiOutputSanitizer;
import com.huixuetong.ai.prompt.AiPromptBuilder;
import com.huixuetong.ai.rag.KnowledgeRagAdvisor;
import com.huixuetong.ai.trace.AiTrace;
import com.huixuetong.ai.trace.TracedToolCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import com.huixuetong.ai.tool.*;
import com.huixuetong.config.ChatClientConfig;
import com.huixuetong.mapper.ChatRecordMapper;
import com.huixuetong.pojo.ChatRecord;
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
    @Autowired private AiTrace aiTraceService;
    @Autowired private KnowledgeRagAdvisor knowledgeRag;
    @Autowired private QuestionAnswerAdvisor questionAnswer;
    @Autowired private SummarizationAdvisor summarization;
    @Autowired private TokenTrackingAdvisor tokenTracking;

    // ===== 数据持久化 =====
    @Autowired private RedisChatHistoryManager historyManager;
    @Autowired private SemanticChatMemory semanticChatMemory;
    @Autowired private com.huixuetong.ai.cache.AiAnswerCache aiAnswerCache;
    @Autowired private ChatRecordMapper chatRecordMapper;

    // ===== 6 个域 Tool（按 Intent 选择性地注册）=====
    @Autowired private StudentTools studentTools;
    @Autowired private EmployeeTools employeeTools;
    @Autowired private ClazzTools clazzTools;
    @Autowired private CourseTools courseTools;
    @Autowired private AffairsTools affairsTools;
    @Autowired private NoticeTools noticeTools;
    @Autowired private SqlQueryTools sqlQueryTools;

    /** 知识库无命中时的固定拒答话术（避免模型编造） */
    private static final String KB_MISS_MESSAGE =
        "知识库暂未收录该问题，建议咨询教务处或相关负责老师。";

    /** 所有模型都不可用时的兜底回复 */
    private static final String[] FALLBACK_MESSAGES = {
        "抱歉，AI助手当前暂时无法提供服务，请稍后再试，或尝试切换其他模型。"
    };

    /** 模型把工具调用当正文吐出来、清洗后无有效内容时的兜底话术 */
    private static final String TOOL_MARKUP_FALLBACK =
        "抱歉，我这次没能完成查询，请换个问法再试一次。";

    // ════════════════════════════════════════════
    //  对外入口
    // ════════════════════════════════════════════



    /**
     * 流式对话入口 — 混合流式
     *
     * 无工具意图（GREETING/TEXT_GEN/KNOWLEDGE_RAG）：token 级真流式，SSE 逐块推送；
     * 工具意图（DATA_STATS/DATA_QUERY/CHAT）：先同步执行工具调用（工具结果必须完整才能生成答案），再分块推送最终答案。
     */
    public Flux<String> stream(String message, Integer userId, String sessionId, String modelType) {
        return stream(message, userId, sessionId, modelType, null);
    }

    public Flux<String> stream(String message, Integer userId, String sessionId, String modelType, AiTrace.Ctx trace) {
        // 1. 安全拦截（抛异常由 Controller 捕获）
        try {
            safeGuard.check(message);
        } catch (PromptInjectionException e) {
            return Flux.just("[安全提醒] 输入包含不安全的指令，已被安全拦截。");
        }

        // 2. 会话标识
        String effectiveSessionId = sessionId != null ? sessionId : "default";

        // 3. 意图路由
        Intent intent = intentRouter.route(message);
        log.info("意图路由: intent={}, question={}", intent, message);
        if (trace != null) trace.setIntent(intent.name());

        // 4. 加载历史
        List<Map<String, String>> history = resolveHistory(userId, effectiveSessionId);
        String summary = null;
        try {
            summary = semanticChatMemory.getSummary(effectiveSessionId);
        } catch (Exception e) {
            // Redis 不可用时降级为"无记忆"，不能让整个问答挂掉
            log.warn("读取会话摘要失败（Redis 不可用？），按无摘要继续: {}", e.getMessage());
        }

        // 5. 知识库 RAG（Intent=KNOWLEDGE_RAG 时预检索）
        String kbContext = "";
        if (intent == Intent.KNOWLEDGE_RAG) {
            kbContext = knowledgeRag.retrieveContext(message);
            // 无来源不回答：检索为空直接拒答，不把空上下文丢给模型去编
            if (kbContext == null || kbContext.isBlank()) {
                if (trace != null) trace.setRagHit(false);
                return Flux.just(KB_MISS_MESSAGE);
            }
        }
        if (trace != null) {
            trace.setRagHit(kbContext != null && !kbContext.isBlank());
            if (kbContext != null && !kbContext.isBlank()) {
                trace.citations.clear();
                trace.citations.addAll(knowledgeRag.searchCitations(message, 3));
            }
        }

        // 6. 构造 Prompt
        String systemPrompt = promptBuilder.build(intent, kbContext, summary);

        // 6.1 构造带上下文的用户消息（修复：history 加载后从未传入模型）
        String userMessage = buildUserMessage(message, history);

        // 7. 选工具（按 Intent 选对应的 Tool Bean；无工具意图返回空数组）
        Object[] tools = resolveTools(intent);

        // 7.1 缓存：仅无工具意图可缓存，key 含会话+上下文指纹，防止串答案
        boolean cacheable = tools.length == 0;
        String cacheKey = cacheable ? buildCacheKey(userId, effectiveSessionId, history, message) : null;
        if (cacheKey != null) {
            String cached = aiAnswerCache.get(cacheKey);
            if (cached != null) {
                log.info("AI缓存命中: userId={}, question={}", userId, message);
                return toSimulatedStream(cached);
            }
        }

        // 8. 无工具意图 → token 级真流式
        if (tools.length == 0) {
            return streamAnswer(message, userId, effectiveSessionId, systemPrompt, userMessage,
                    modelType, intent, summary, cacheKey, trace);
        }

        // 9. 工具意图 → 同步执行工具（Spring AI 自动完成 tool_use 循环），再分块推送最终答案
        try {
            ChatClient client = getChatClientWithFallback(modelType, trace);

            // LongCat 已验证支持 Function Calling（返回 finish_reason:"tool_calls"）
            org.springframework.ai.chat.model.ChatResponse chatResponse = client.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .tools(wrapTools(tools, trace))
                .call()
                .chatResponse();

            // 从 ChatResponse 提取最终文本（工具已由 Spring AI 自动执行）
            String finalAnswer = "";
            if (chatResponse != null && chatResponse.getResult() != null
                && chatResponse.getResult().getOutput() != null) {
                finalAnswer = chatResponse.getResult().getOutput().getText();
            }

            finalAnswer = AiOutputSanitizer.sanitize(
                    maskSensitiveData(finalAnswer != null ? finalAnswer : ""));
            if (finalAnswer.isEmpty()) {
                log.warn("工具意图回答清洗后为空（疑似工具标记泄漏）: intent={}, userId={}", intent, userId);
                return toSimulatedStream(TOOL_MARKUP_FALLBACK);
            }

            // 持久化 + 缓存
            if (!finalAnswer.isEmpty()) {
                asyncPersist(userId, effectiveSessionId, message, finalAnswer,
                        systemPrompt, modelType, summary, intent, cacheKey);
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
                                      Intent intent, String summary, String cacheKey, AiTrace.Ctx trace) {
        try {
            ChatClient client = getChatClientWithFallback(modelType, trace);
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
                    // 流式已推送的 chunk 无法回收，这里只保证落库/缓存的是清洗后的文本
                    String full = AiOutputSanitizer.sanitize(collected.toString());
                    if (!full.isEmpty()) {
                        asyncPersist(userId, sessionId, message, full,
                                systemPrompt, modelType, summary, intent, cacheKey);
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
        return chat(message, history, modelType, userId, sessionId, null);
    }

    /**
     * 同步对话入口（测试用）
     */
    public String chat(String message, List<Map<String, String>> history,
                       String modelType, Integer userId, String sessionId, AiTrace.Ctx trace) {
        // 安全拦截
        safeGuard.check(message);

        // 意图路由
        Intent intent = intentRouter.route(message);
        if (trace != null) trace.setIntent(intent.name());

        // 知识库检索（无命中直接拒答）
        String kbContext = "";
        if (intent == Intent.KNOWLEDGE_RAG) {
            kbContext = knowledgeRag.retrieveContext(message);
            if (kbContext == null || kbContext.isBlank()) {
                if (trace != null) trace.setRagHit(false);
                return KB_MISS_MESSAGE;
            }
        }
        if (trace != null) {
            trace.setRagHit(kbContext != null && !kbContext.isBlank());
            if (kbContext != null && !kbContext.isBlank()) {
                trace.citations.clear();
                trace.citations.addAll(knowledgeRag.searchCitations(message, 3));
            }
        }
        String systemPrompt = promptBuilder.build(intent, kbContext, null);

        Object[] tools = resolveTools(intent);
        // 仅无工具意图可缓存（key 含会话+上下文指纹）
        String chatCacheKey = tools.length == 0
                ? buildCacheKey(userId, sessionId != null ? sessionId : "default",
                        history != null ? history : List.of(), message)
                : null;
        if (chatCacheKey != null) {
            String cached = aiAnswerCache.get(chatCacheKey);
            if (cached != null) return cached;
        }
        ChatClient client = getChatClientWithFallback(modelType, trace);

        List<ToolCallback> callbacks = wrapTools(tools, trace);
        var spec = client.prompt()
            .system(systemPrompt)
            .user(message);
        if (!callbacks.isEmpty()) {
            spec = spec.tools(callbacks);
        }
        org.springframework.ai.chat.model.ChatResponse chatResponse = spec.call().chatResponse();

        String answer = "";
        if (chatResponse != null && chatResponse.getResult() != null
            && chatResponse.getResult().getOutput() != null) {
            answer = AiOutputSanitizer.sanitize(
                    maskSensitiveData(chatResponse.getResult().getOutput().getText()));
        }

        // 持久化 + 缓存
        if (!answer.isEmpty()) {
            asyncPersist(userId, sessionId != null ? sessionId : "default", message, answer,
                    systemPrompt, modelType, null, intent, chatCacheKey);
        } else if (chatResponse != null) {
            log.warn("同步回答清洗后为空（疑似工具标记泄漏）: intent={}, userId={}", intent, userId);
            return TOOL_MARKUP_FALLBACK;
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
            case DATA_STATS -> new Object[]{ studentTools, employeeTools, sqlQueryTools };
            case DATA_QUERY -> new Object[]{ studentTools, employeeTools, clazzTools, courseTools, affairsTools, sqlQueryTools };
            case KNOWLEDGE_RAG -> new Object[]{}; // RAG 上下文已注入 system prompt，无需工具，可走 token 级流式
            case TEXT_GEN -> new Object[]{};
            case GREETING -> new Object[]{};
            case CHAT -> new Object[]{ studentTools, employeeTools, clazzTools, courseTools, affairsTools, noticeTools, sqlQueryTools };
        };
    }

    /**
     * 把 @Tool 对象包装成带埋点的 ToolCallback
     * （不用 AOP：Spring AI 直接反射调用目标对象，CGLIB 代理会被绕过）
     */
    private List<ToolCallback> wrapTools(Object[] tools, AiTrace.Ctx trace) {
        if (tools == null || tools.length == 0) return List.of();
        ToolCallback[] callbacks = ToolCallbacks.from(tools);
        List<ToolCallback> wrapped = new ArrayList<>(callbacks.length);
        for (ToolCallback cb : callbacks) {
            wrapped.add(new TracedToolCallback(cb, aiTraceService, trace));
        }
        return wrapped;
    }

    /**
     * 带降级的 ChatClient 获取
     */
    private ChatClient getChatClientWithFallback(String modelType, AiTrace.Ctx trace) {
        String requested = (modelType == null || modelType.trim().isEmpty())
                ? null : modelType.trim().toLowerCase();
        try {
            ChatClient client = chatClientConfig.getChatClient(requested);
            if (trace != null) {
                trace.setModelType(requested != null ? requested : chatClientConfig.defaultModelType());
            }
            return client;
        } catch (Exception e) {
            String fallbackType = chatClientConfig.defaultModelType();
            log.warn("主模型({})创建失败，降级到默认模型({}): {}", modelType, fallbackType, e.getMessage());
            if (trace != null) {
                trace.setFallback(true);
                trace.setModelType(fallbackType);
            }
            if (fallbackType.equalsIgnoreCase(requested)) {
                // 请求的就是默认模型，重试同一个没有意义
                throw e;
            }
            return chatClientConfig.getChatClient(fallbackType);
        }
    }

    /**
     * 上下文感知缓存 key：userId + sessionId + 最近4条消息指纹 + 问题
     * 同一问题在不同上下文/会话中互不污染；数据类意图不调用本方法
     */
    private String buildCacheKey(Integer userId, String sessionId,
                                 List<Map<String, String>> history, String message) {
        StringBuilder ctx = new StringBuilder();
        int from = Math.max(0, history.size() - 4);
        for (int i = from; i < history.size(); i++) {
            Map<String, String> m = history.get(i);
            if (m != null) {
                ctx.append(m.get("role")).append('=').append(m.get("content")).append('|');
            }
        }
        ctx.append("|sid=").append(sessionId == null ? "" : sessionId);
        String prefix = userId != null ? userId + ":" : "";
        return prefix + Integer.toHexString(ctx.toString().hashCode()) + ":" + message.trim().toLowerCase();
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
            String role = String.valueOf(msg.get("role"));
            String content = String.valueOf(msg.get("content"));
            if ("user".equals(role)) {
                sb.append("用户: ").append(content).append("\n");
            } else if ("assistant".equals(role)) {
                sb.append("助手: ").append(content).append("\n");
            }
        }
        sb.append("\n注意：请结合上面的历史对话理解当前问题的指代（如\"男的\"可能指上一问中的男学员），但只回答当前问题本身，不要重复回答历史问题。\n");
        sb.append("当前问题: ").append(currentQuestion);
        return sb.toString();
    }

    /**
     * 数据脱敏（委托给 DataMasker 工具类）
     */
    private String maskSensitiveData(String text) {
        return com.huixuetong.ai.common.DataMasker.mask(text);
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
        if (userId == null || sessionId == null || sessionId.isEmpty()) return new ArrayList<>();
        try {
            List<Map<String, String>> redisHistory = historyManager.getHistory(userId, sessionId);
            return redisHistory != null ? redisHistory : new ArrayList<>();
        } catch (Exception e) {
            // Redis 不可用时降级为"无记忆"，保证问答本身可用
            log.warn("加载会话历史失败（Redis 不可用？），按无历史继续: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ════════════════════════════════════════════
    //  异步持久化
    // ════════════════════════════════════════════

    private void asyncPersist(Integer userId, String sessionId, String message,
                              String answer, String systemPrompt, String modelType,
                              String summary, Intent intent, String cacheKey) {
        try {
            if (answer == null || answer.isEmpty()) return;
            // 避免把占位符和错误兜底消息写入缓存
            if (answer.equals("[streamed]") || answer.startsWith("[错误]")) return;
            if (answer.contains("模型未返回有效响应") || answer.contains("请稍后重试")) return;
            if (AiOutputSanitizer.containsToolMarkup(answer)) return;

            // 1) MySQL 落库 + Token 统计（不依赖 Redis，优先保证）
            asyncSaveChatRecord(userId, sessionId, message, answer);
            int promptChars = systemPrompt.length() + message.length();
            int answerChars = answer.length();
            tokenTracking.record(userId, sessionId, modelType,
                    Math.max(1, (int) (promptChars / 1.3)),
                    Math.max(1, (int) (answerChars / 1.3)),
                    Math.max(2, (int) ((promptChars + answerChars) / 1.3)));

            // 2) 缓存 + 会话历史（Redis 故障只降级，不能影响上面的落库）
            try {
                if (cacheKey != null) {
                    aiAnswerCache.put(cacheKey, answer);
                }
                if (userId != null && sessionId != null && !sessionId.isEmpty()) {
                    historyManager.saveRound(userId, sessionId, message, answer);
                }
            } catch (Exception e) {
                log.warn("写缓存/会话历史失败（Redis 不可用？），已跳过: {}", e.getMessage());
            }

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
