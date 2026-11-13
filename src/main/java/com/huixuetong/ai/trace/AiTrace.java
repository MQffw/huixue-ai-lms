package com.huixuetong.ai.trace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huixuetong.mapper.AiToolLogMapper;
import com.huixuetong.mapper.AiTraceMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 全链路追踪（Observability）
 *
 * 与旧实现的区别：
 *   旧版只靠 ThreadLocal，而流式回答的完成/异常回调跑在 Reactor 线程上，
 *   ThreadLocal 会丢，所以 AiTrace 一直没被接进主链路。
 *   现在改成"显式上下文"：
 *     - Controller 用 begin() 创建 Ctx 并传给编排层；
 *     - 编排层把 intent / model / ragHit 写进 Ctx；
 *     - 工具调用由 AOP 在请求线程内记录（此期间 ThreadLocal 仍指向同一个 Ctx）；
 *     - 流结束时 Controller 用 end(ctx, ...) 落表，线程无关。
 *
 * 表：hxt_ai_trace（一次对话一行）、hxt_tool_call_log（一次工具调用一行）
 */
@Slf4j
@Component
public class AiTrace {

    private static final String MDC_KEY = "aiTraceId";
    private static final int MAX_QUESTION = 500;
    private static final int MAX_ERROR = 500;
    private static final int MAX_ARGS = 1000;

    private static final ThreadLocal<Ctx> CURRENT = new ThreadLocal<>();

    @Autowired
    private AiTraceMapper traceMapper;
    @Autowired
    private AiToolLogMapper toolLogMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 一次对话的追踪上下文；字段由编排层逐步填充 */
    public static class Ctx {
        public final String traceId;
        public final Integer userId;
        public final String sessionId;
        public final String question;
        public final long startTime = System.currentTimeMillis();

        public volatile String intent;
        public volatile String modelType;
        public volatile boolean fallback;
        public volatile boolean ragHit;

        public final AtomicInteger toolCalls = new AtomicInteger();
        public final List<String> toolNames = Collections.synchronizedList(new ArrayList<>());
        /** RAG 命中的来源，供 SSE cite 事件下发 */
        public final List<Map<String, Object>> citations = Collections.synchronizedList(new ArrayList<>());

        Ctx(String traceId, Integer userId, String sessionId, String question) {
            this.traceId = traceId;
            this.userId = userId;
            this.sessionId = sessionId;
            this.question = question;
        }

        public Ctx setIntent(String intent) {
            this.intent = intent;
            return this;
        }

        public Ctx setModelType(String modelType) {
            this.modelType = modelType;
            return this;
        }

        public Ctx setFallback(boolean fallback) {
            this.fallback = fallback;
            return this;
        }

        public Ctx setRagHit(boolean ragHit) {
            this.ragHit = ragHit;
            return this;
        }
    }

    /** 开启一次追踪，并绑定到当前线程（供工具 AOP 读取） */
    public Ctx begin(String traceId, Integer userId, String sessionId, String question) {
        Ctx ctx = new Ctx(traceId, userId, sessionId, cut(question, MAX_QUESTION));
        CURRENT.set(ctx);
        MDC.put(MDC_KEY, traceId.substring(0, Math.min(8, traceId.length())));
        return ctx;
    }

    public Ctx current() {
        return CURRENT.get();
    }

    /** 解除线程绑定（工具调用结束后必须调用，避免线程池串上下文） */
    public void unbind() {
        CURRENT.remove();
        MDC.remove(MDC_KEY);
    }

    /** 记录一次工具调用（线程绑定版，供 AOP 使用） */
    public void addToolCall(String toolName, String args, boolean success, int durationMs) {
        recordToolCall(CURRENT.get(), toolName, args, success, durationMs);
    }

    /** 记录一次工具调用（显式上下文版，供工具回调包装使用） */
    public void recordToolCall(Ctx ctx, String toolName, String args, boolean success, int durationMs) {
        if (ctx == null) return;

        ctx.toolCalls.incrementAndGet();
        ctx.toolNames.add(toolName);

        if (toolLogMapper == null) return;
        try {
            toolLogMapper.insert(ctx.userId == null ? 0 : ctx.userId, ctx.sessionId,
                    toolName, cut(args, MAX_ARGS), success, durationMs);
        } catch (Exception e) {
            log.warn("工具调用日志落表失败: tool={}, err={}", toolName, e.getMessage());
        }
    }

    /** 结束追踪并落 hxt_ai_trace */
    public void end(Ctx ctx, int answerChars, String errorMsg) {
        if (ctx == null) return;

        int latencyMs = (int) (System.currentTimeMillis() - ctx.startTime);
        String status = (errorMsg == null) ? "SUCCESS" : "ERROR";

        try {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("tools", ctx.toolNames);
            detail.put("intent", ctx.intent);
            detail.put("model", ctx.modelType);
            detail.put("ragHit", ctx.ragHit);
            detail.put("fallback", ctx.fallback);

            traceMapper.insert(ctx.traceId, ctx.userId, ctx.sessionId, ctx.question,
                    ctx.intent, ctx.modelType, ctx.fallback, ctx.toolCalls.get(),
                    ctx.ragHit, answerChars, latencyMs, status,
                    cut(errorMsg, MAX_ERROR), objectMapper.writeValueAsString(detail));
        } catch (Exception e) {
            log.warn("AiTrace 落表失败: traceId={}, err={}", ctx.traceId, e.getMessage());
        }

        log.info("AiTrace 完成: traceId={}, intent={}, latency={}ms, tools={}, status={}",
                ctx.traceId, ctx.intent, latencyMs, ctx.toolCalls.get(), status);
    }

    private static String cut(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
