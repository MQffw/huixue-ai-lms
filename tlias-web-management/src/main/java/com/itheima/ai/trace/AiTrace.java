package com.itheima.ai.trace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.mapper.AiTraceMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 全链路追踪（Observability）
 *
 * 设计：ThreadLocal 上下文 + MDC 联动日志
 *     - 每个请求一个 traceId，所有日志都带上 prefix
 *     - 单次请求中可追加 ToolCalls / RAG Hits / 错误堆栈等
 *     - 请求结束时落表 tlias_ai_trace
 *
 * 用法：
 *   AiTrace.start(userId, sessionId, question);
 *   AiTrace.addToolCall("countStudentsByDegree");
 *   AiTrace.setRagHit(true);
 *   AiTrace.end(status, answer, errorMsg);
 */
@Slf4j
@Component
public class AiTrace {

    private static final String MDC_KEY = "aiTraceId";

    private static final ThreadLocal<TraceCtx> CTX = new ThreadLocal<>();

    @Autowired
    private AiTraceMapper aiTraceMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ════ 单次请求操作 ════

    /**
     * 启动一次追踪，返回 traceId
     */
    public String start(Integer userId, String sessionId, String question) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        TraceCtx ctx = new TraceCtx();
        ctx.traceId = traceId;
        ctx.userId = userId;
        ctx.sessionId = sessionId;
        ctx.question = question;
        ctx.startTime = System.currentTimeMillis();
        ctx.status = "SUCCESS";
        CTX.set(ctx);
        MDC.put(MDC_KEY, traceId.substring(0, 8));
        return traceId;
    }

    /**
     * 追加一次工具调用
     */
    public void addToolCall(String toolName) {
        TraceCtx ctx = CTX.get();
        if (ctx == null) return;
        ctx.toolCalls.incrementAndGet();
        ctx.toolNames.add(toolName);
    }

    /**
     * 标记 RAG 命中
     */
    public void setRagHit(boolean hit) {
        TraceCtx ctx = CTX.get();
        if (ctx == null) return;
        ctx.ragHit = hit;
    }

    /**
     * 设置本次请求意图
     */
    public void setIntent(String intent) {
        TraceCtx ctx = CTX.get();
        if (ctx == null) return;
        ctx.intent = intent;
    }

    /** 设置实际使用的模型 */
    public void setModelType(String modelType) {
        TraceCtx ctx = CTX.get();
        if (ctx == null) return;
        ctx.modelType = modelType;
    }

    /** 标记使用了降级模型 */
    public void setFallback(boolean fallback) {
        TraceCtx ctx = CTX.get();
        if (ctx == null) return;
        ctx.fallback = fallback;
    }

    /**
     * 结束追踪并异步落表
     * @param answer     最终回答（null 表示失败）
     * @param errorMsg   异常消息（null 表示无异常）
     */
    public void end(String answer, String errorMsg) {
        TraceCtx ctx = CTX.get();
        if (ctx == null) return;

        ctx.status = errorMsg == null ? "SUCCESS" : "ERROR";
        if (errorMsg != null) ctx.errorMsg = errorMsg.length() > 500 ? errorMsg.substring(0, 500) : errorMsg;
        ctx.answerChars = (answer != null) ? answer.length() : 0;
        ctx.latencyMs = (int) (System.currentTimeMillis() - ctx.startTime);

        // 异步落表（非阻塞）
        final TraceCtx finalCtx = ctx;
        try {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("toolNames", finalCtx.toolNames);
            detail.put("userId", finalCtx.userId);
            String detailJson = objectMapper.writeValueAsString(detail);
            aiTraceMapper.insert(
                    finalCtx.traceId, finalCtx.userId, finalCtx.sessionId,
                    finalCtx.question, finalCtx.intent, finalCtx.modelType,
                    finalCtx.fallback, finalCtx.toolCalls.get(),
                    finalCtx.ragHit, finalCtx.answerChars, finalCtx.latencyMs,
                    finalCtx.status, finalCtx.errorMsg, detailJson);
        } catch (Exception e) {
            log.warn("AiTrace 落表失败: traceId={}, err={}", finalCtx.traceId, e.getMessage());
        }

        log.info("AiTrace 完成: traceId={}, latency={}ms, toolCalls={}, status={}",
                finalCtx.traceId, finalCtx.latencyMs, finalCtx.toolCalls.get(), finalCtx.status);
        CTX.remove();
        MDC.remove(MDC_KEY);
    }

    // ════ 内部上下文 ════

    private static class TraceCtx {
        String traceId;
        Integer userId;
        String sessionId;
        String question;
        String intent;
        String modelType;
        boolean fallback;
        boolean ragHit;
        AtomicInteger toolCalls = new AtomicInteger();
        List<String> toolNames = new ArrayList<>();
        int answerChars;
        int latencyMs;
        String status;
        String errorMsg;
        long startTime;
    }
}
