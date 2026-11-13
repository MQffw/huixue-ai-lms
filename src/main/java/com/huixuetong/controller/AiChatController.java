package com.huixuetong.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huixuetong.ai.cache.AiAnswerCache;
import com.huixuetong.ai.orchestrator.AiOrchestratorService;
import com.huixuetong.ai.trace.AiTrace;
import com.huixuetong.pojo.Result;
import com.huixuetong.utils.CurrentHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 聊天控制器（Phase 3 Orchestrator 版）
 * - 直接注入 AiOrchestratorService
 * - 混合流式：无工具意图 token 级流式；工具意图先执行工具再分块推送（SSE）
 * - 统一异常处理
 */
@Tag(name = "AI智能助手", description = "AI对话接口，支持多模型切换、原生Flux流式输出、Intent路由、Agent工具调用")
@Slf4j
@RequestMapping("/ai")
@RestController
@Validated
public class AiChatController {

    @Autowired
    private AiOrchestratorService orchestrator;
    @Autowired
    private AiAnswerCache aiAnswerCache;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AiTrace aiTrace;

    /**
     * AI聊天请求DTO（复用原结构）
     */
    public static class AiChatRequest {
        private String message;
        private List<Map<String, String>> history;
        private String modelType;   // 为空时由 ChatClientConfig 取注册表默认模型
        private String sessionId;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public List<Map<String, String>> getHistory() { return history; }
        public void setHistory(List<Map<String, String>> history) { this.history = history; }
        public String getModelType() { return modelType; }
        public void setModelType(String modelType) { this.modelType = modelType; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        @Override
        public String toString() {
            return "AiChatRequest{message='" + (message != null ? message.substring(0, Math.min(message.length(), 50)) : "null")
                    + "', modelType='" + modelType + "', sessionId='" + sessionId
                    + "', historySize=" + (history != null ? history.size() : 0) + "}";
        }
    }

    /**
     * 流式聊天（SSE 命名事件）
     *
     * 事件协议（前端按 event 分发，不再靠正则猜内容）：
     *   event: meta   {"traceId","sessionId","model"}
     *   event: delta  "文本片段"（\n 用 §n§ 占位，避免破坏 SSE 帧）
     *   event: error  {"message"}
     *   event: done   {"latencyMs","chars"}
     */
    @Operation(summary = "AI流式对话（SSE）", description = "命名事件 meta/delta/error/done，支持工具调用与多模型切换")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody AiChatRequest request) {
        validateRequest(request);

        log.info("AI流式聊天请求: {}", request);

        Integer userId = CurrentHolder.getId();
        String sessionId = (request.getSessionId() != null && !request.getSessionId().isEmpty())
                ? request.getSessionId()
                : "user-" + userId + "-default";
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String model = (request.getModelType() == null || request.getModelType().isEmpty())
                ? "default" : request.getModelType();
        long start = System.currentTimeMillis();
        AiTrace.Ctx trace = aiTrace.begin(traceId, userId, sessionId, request.getMessage());

        SseEmitter emitter = new SseEmitter(120_000L);

        // 1. 先发 meta：前端据此拿到 traceId，后续可上报/排查
        try {
            emitter.send(SseEmitter.event().name("meta")
                    .data(json(Map.of("traceId", traceId, "sessionId", sessionId, "model", model))));
        } catch (IOException e) {
            log.warn("发送 meta 事件失败: {}", e.getMessage());
            aiTrace.end(trace, 0, "meta send failed");
            aiTrace.unbind();
            emitter.complete();
            return emitter;
        }

        // 2. 订阅编排层：delta 逐块推送，结束发 done，异常发 error
        StringBuilder collected = new StringBuilder();
        Flux<String> flux;
        try {
            flux = orchestrator.stream(request.getMessage(), userId, sessionId, request.getModelType(), trace);
        } finally {
            // 工具调用已在 stream() 内同步执行完毕，解除线程绑定避免线程池串上下文
            aiTrace.unbind();
        }

        // 3. RAG 命中的来源：先下发 cite，前端在气泡下方渲染"来源"
        if (!trace.citations.isEmpty()) {
            try {
                emitter.send(SseEmitter.event().name("cite").data(json(trace.citations)));
            } catch (Exception e) {
                log.warn("发送 cite 事件失败: {}", e.getMessage());
            }
        }

        Disposable subscription = flux.subscribe(
                chunk -> {
                    if (chunk == null) return;
                    collected.append(chunk);
                    try {
                        emitter.send(SseEmitter.event().name("delta")
                                .data(chunk.replace("\n", "§n§")));
                    } catch (Exception e) {
                        log.warn("SSE delta 发送失败（客户端可能已断开）: {}", e.getMessage());
                    }
                },
                error -> {
                    log.error("AI 流式响应异常: traceId={}, reason={}", traceId, error.getMessage(), error);
                    aiTrace.end(trace, collected.length(), error.getMessage());
                    try {
                        emitter.send(SseEmitter.event().name("error")
                                .data(json(Map.of("message", "AI 服务暂时不可用，请稍后重试"))));
                    } catch (Exception ignored) {
                        // 客户端已断开
                    }
                    emitter.complete();
                },
                () -> {
                    aiTrace.end(trace, collected.length(), null);
                    try {
                        emitter.send(SseEmitter.event().name("done")
                                .data(json(Map.of("latencyMs", System.currentTimeMillis() - start,
                                                  "chars", collected.length()))));
                    } catch (Exception ignored) {
                        // 客户端已断开
                    }
                    emitter.complete();
                }
        );

        emitter.onCompletion(subscription::dispose);
        emitter.onTimeout(() -> {
            log.warn("SSE连接超时: traceId={}, userId={}, sessionId={}", traceId, userId, sessionId);
            subscription.dispose();
            emitter.complete();
        });
        emitter.onError(t -> subscription.dispose());

        return emitter;
    }

    /** 小对象转 JSON 字符串（meta/done/error 事件用，避免依赖消息转换器） */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("SSE 事件序列化失败: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 加载对话历史
     */
    @Operation(summary = "加载对话历史")
    @GetMapping("/history")
    public Result loadHistory(@RequestParam String sessionId) {
        Integer userId = CurrentHolder.getId();
        var history = orchestrator.getHistory(userId, sessionId);
        return Result.success(history);
    }

    /**
     * 同步聊天（测试用）
     */
    @Operation(summary = "AI同步对话", description = "同步返回完整响应，适用于测试场景")
    @PostMapping("/chat/sync")
    public Result chatSync(@RequestBody AiChatRequest request) {
        validateRequest(request);

        log.info("AI同步聊天请求: {}", request);
        Integer userId = CurrentHolder.getId();
        String sessionId = (request.getSessionId() != null && !request.getSessionId().isEmpty())
                ? request.getSessionId()
                : "user-" + userId + "-default";

        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        AiTrace.Ctx trace = aiTrace.begin(traceId, userId, sessionId, request.getMessage());
        String response;
        try {
            response = orchestrator.chat(request.getMessage(), request.getHistory(),
                    request.getModelType(), userId, sessionId, trace);
            aiTrace.end(trace, response == null ? 0 : response.length(), null);
        } catch (RuntimeException e) {
            aiTrace.end(trace, 0, e.getMessage());
            throw e;
        } finally {
            aiTrace.unbind();
        }
        return Result.success(response);
    }

    /**
     * 清除 AI 回答缓存（新建对话时调用）
     */
    @Operation(summary = "清除AI缓存", description = "新建对话时清除Redis缓存")
    @PostMapping("/cache/clear")
    public Result clearCache() {
        Integer userId = CurrentHolder.getId();
        log.info("清除AI缓存: userId={}", userId);
        aiAnswerCache.clearByUser(String.valueOf(CurrentHolder.getId()));
        return Result.success();
    }

    // ======================== 辅助 ========================

    private void validateRequest(AiChatRequest request) {
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("消息不能为空");
        }
        if (request.getMessage().length() > 1000) {
            throw new IllegalArgumentException("消息长度不能超过1000字符");
        }
        if (request.getHistory() != null && request.getHistory().size() > 20) {
            throw new IllegalArgumentException("历史记录不能超过20条");
        }
    }
}
