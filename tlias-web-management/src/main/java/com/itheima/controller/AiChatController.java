package com.itheima.controller;

import com.itheima.ai.cache.AiAnswerCache;
import com.itheima.ai.orchestrator.AiOrchestratorService;
import com.itheima.pojo.Result;
import com.itheima.utils.CurrentHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

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

    /**
     * AI聊天请求DTO（复用原结构）
     */
    public static class AiChatRequest {
        private String message;
        private List<Map<String, String>> history;
        private String modelType = "longcat";
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
     * 流式聊天（Phase 4 混合流式版）
     * - 无工具意图：Spring AI 原生 Flux<String> token 级流；工具意图：工具执行后分块推送
     * - 错误处理：SSE 兼容格式
     */
    @Operation(summary = "AI流式对话（SSE）", description = "支持DeepSeek/Mimo/LongCat三模型切换，流式输出，Intent路由+工具调用")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody AiChatRequest request) {
        validateRequest(request);

        log.info("AI流式聊天请求: {}", request);

        Integer userId = CurrentHolder.getId();
        String sessionId = (request.getSessionId() != null && !request.getSessionId().isEmpty())
                ? request.getSessionId()
                : "user-" + userId + "-default";

        // 调用 Orchestrator 获取原生 Flux
        Flux<String> flux = orchestrator.stream(request.getMessage(),
                userId, sessionId, request.getModelType());

        SseEmitter emitter = new SseEmitter(120_000L);

        flux
            .doOnNext(chunk -> {
                try {
                    // SSE协议中 \n 会被当作事件分隔符丢失，用占位符替代
                    String safe = chunk.replace("\n", "§n§");
                    emitter.send(safe);
                } catch (Exception e) {
                    log.warn("发送SSE数据块失败", e);
                }
            })
            .doOnError(error -> {
                log.error("AI Agent 流式响应异常: {}", error.getMessage(), error);
                try {
                    emitter.send("抱歉，AI助手当前暂时无法提供服务，请稍后再试。");
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            .doOnComplete(() -> {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    // already completed
                }
            })
            .subscribe();

        emitter.onTimeout(() -> log.warn("SSE连接超时: userId={}, sessionId={}", userId, sessionId));

        return emitter;
    }

    /**
     * 加载对话历史
     */
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

        String response = orchestrator.chat(request.getMessage(), request.getHistory(),
                request.getModelType(), userId, sessionId);
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
        aiAnswerCache.clear();
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
