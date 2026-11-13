package com.huixuetong.ai.trace;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * 工具回调装饰器：在不侵入业务 Tool 的前提下，记录每次工具调用的
 * 名称 / 参数 / 耗时 / 成功与否，写入 hxt_tool_call_log 并累加到 trace。
 *
 * 为什么不用 AOP：Spring AI 解析 @Tool 时会拿到目标对象直接反射调用，
 * CGLIB 代理被绕过，@Around 切不到（已实测 tool_calls=0）。
 */
public class TracedToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final AiTrace aiTrace;
    private final AiTrace.Ctx ctx;

    public TracedToolCallback(ToolCallback delegate, AiTrace aiTrace, AiTrace.Ctx ctx) {
        this.delegate = delegate;
        this.aiTrace = aiTrace;
        this.ctx = ctx;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        long start = System.currentTimeMillis();
        boolean success = true;
        try {
            return delegate.call(toolInput);
        } catch (RuntimeException e) {
            success = false;
            throw e;
        } finally {
            record(toolInput, success, start);
        }
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        long start = System.currentTimeMillis();
        boolean success = true;
        try {
            return delegate.call(toolInput, toolContext);
        } catch (RuntimeException e) {
            success = false;
            throw e;
        } finally {
            record(toolInput, success, start);
        }
    }

    private void record(String input, boolean success, long start) {
        try {
            aiTrace.recordToolCall(ctx, getToolDefinition().name(), input, success,
                    (int) (System.currentTimeMillis() - start));
        } catch (Exception ignored) {
            // 埋点失败不影响工具执行
        }
    }
}
