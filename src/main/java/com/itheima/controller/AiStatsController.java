package com.itheima.controller;

import com.itheima.mapper.AiStatsMapper;
import com.itheima.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ai-stats")
public class AiStatsController {

    @Autowired
    private AiStatsMapper aiStatsMapper;

    /** 总概览统计 */
    @GetMapping("/overview")
    public Result getOverview() {
        try {
            Map<String, Object> overview = aiStatsMapper.selectOverview();
            return Result.success(overview);
        } catch (Exception e) {
            log.error("获取AI总览统计失败", e);
            return Result.error(500, "获取统计数据失败");
        }
    }

    /** 24小时 token 使用趋势（柱状图数据） */
    @GetMapping("/tokens-24h")
    public Result getTokens24h() {
        try {
            // 确保有连续的 24 个小时数据（无数据的时段补 0）
            List<Map<String, Object>> rawData = aiStatsMapper.selectHourlyTokens24h();
            return Result.success(fillHourlyData(rawData));
        } catch (Exception e) {
            log.error("获取24小时token趋势失败", e);
            return Result.error(500, "获取数据失败");
        }
    }

    /** 模型使用占比（饼图数据） */
    @GetMapping("/model-distribution")
    public Result getModelDistribution() {
        try {
            List<Map<String, Object>> distribution = aiStatsMapper.selectModelDistribution();
            return Result.success(distribution);
        } catch (Exception e) {
            log.error("获取模型占比失败", e);
            return Result.error(500, "获取数据失败");
        }
    }

    /**
     * 将数据库返回的稀疏小时数据补全为连续 24 小时（无数据的时段补 0）
     */
    private List<Map<String, Object>> fillHourlyData(List<Map<String, Object>> rawData) {
        // 构建 hour -> data 索引
        java.util.Map<String, Map<String, Object>> dataMap = new java.util.LinkedHashMap<>();
        for (Map<String, Object> row : rawData) {
            dataMap.put(String.valueOf(row.get("hour")), row);
        }

        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.HOUR_OF_DAY, -23); // 从24小时前开始
        for (int i = 0; i < 24; i++) {
            String hour = String.format("%02d:00", cal.get(java.util.Calendar.HOUR_OF_DAY));
            Map<String, Object> data = dataMap.get(hour);
            if (data == null) {
                java.util.Map<String, Object> empty = new java.util.LinkedHashMap<>();
                empty.put("hour", hour);
                empty.put("totalTokens", 0L);
                empty.put("promptTokens", 0L);
                empty.put("completionTokens", 0L);
                result.add(empty);
            } else {
                result.add(data);
            }
            cal.add(java.util.Calendar.HOUR_OF_DAY, 1);
        }
        return result;
    }
}
