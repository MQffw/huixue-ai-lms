package com.huixuetong.controller;

import com.huixuetong.mapper.AiTraceMapper;
import com.huixuetong.pojo.PageResult;
import com.huixuetong.pojo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 调用日志（hxt_ai_trace）
 * 列表 + 详情（详情附带同会话最近的工具调用）
 */
@Slf4j
@RestController
@RequestMapping("/ai-logs")
@Tag(name = "AI调用日志", description = "AI 全链路追踪查询")
public class AiLogController {

    @Autowired
    private AiTraceMapper aiTraceMapper;

    @Operation(summary = "调用日志分页")
    @GetMapping
    public Result page(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) String intent,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String keyword) {
        int size = Math.min(Math.max(pageSize, 1), 100);
        int offset = (Math.max(page, 1) - 1) * size;

        List<Map<String, Object>> rows = aiTraceMapper.findPage(intent, status, keyword, offset, size);
        long total = aiTraceMapper.countPage(intent, status, keyword);
        return Result.success(new PageResult<>(total, rows));
    }

    @Operation(summary = "调用日志详情")
    @GetMapping("/{traceId}")
    public Result detail(@PathVariable String traceId) {
        Map<String, Object> trace = aiTraceMapper.findByTraceId(traceId);
        if (trace == null) return Result.error(404, "trace 不存在");

        Map<String, Object> data = new HashMap<>(trace);
        Integer userId = trace.get("user_id") == null ? null : ((Number) trace.get("user_id")).intValue();
        Object sessionId = trace.get("session_id");
        data.put("toolLogs", aiTraceMapper.findToolLogs(userId, sessionId == null ? null : sessionId.toString()));
        return Result.success(data);
    }
}
