package com.itheima.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface AiStatsMapper {

    /**
     * 24小时 token 使用趋势（按小时分组）
     */
    @Select("""
            SELECT
              DATE_FORMAT(record_time, '%H:00') AS hour,
              CAST(SUM(total_tokens) AS UNSIGNED) AS totalTokens,
              CAST(SUM(prompt_tokens) AS UNSIGNED) AS promptTokens,
              CAST(SUM(completion_tokens) AS UNSIGNED) AS completionTokens
            FROM tlias_token_usage
              WHERE record_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
            GROUP BY DATE_FORMAT(record_time, '%H:00')
            ORDER BY hour ASC
            """)
    @Results({
        @Result(property = "hour", column = "hour"),
        @Result(property = "totalTokens", column = "totalTokens"),
        @Result(property = "promptTokens", column = "promptTokens"),
        @Result(property = "completionTokens", column = "completionTokens")
    })
    List<Map<String, Object>> selectHourlyTokens24h();

    /**
     * 模型使用占比
     */
    @Select("""
            SELECT
              model_name AS model,
              CAST(SUM(total_tokens) AS UNSIGNED) AS tokenCount,
              CAST(COUNT(DISTINCT session_id) AS UNSIGNED) AS sessionCount
            FROM tlias_token_usage
            GROUP BY model_name
            ORDER BY tokenCount DESC
            """)
    @Results({
        @Result(property = "model", column = "model"),
        @Result(property = "tokenCount", column = "tokenCount"),
        @Result(property = "sessionCount", column = "sessionCount")
    })
    List<Map<String, Object>> selectModelDistribution();

    /**
     * 总概览统计
     */
    @Select("""
            SELECT
              CAST(COALESCE(SUM(total_tokens), 0) AS UNSIGNED) AS totalTokens,
              CAST(COALESCE(SUM(prompt_tokens), 0) AS UNSIGNED) AS totalPromptTokens,
              CAST(COALESCE(SUM(completion_tokens), 0) AS UNSIGNED) AS totalCompletionTokens,
              CAST(COUNT(DISTINCT session_id) AS UNSIGNED) AS totalSessions,
              CAST(COUNT(DISTINCT user_id) AS UNSIGNED) AS totalUsers
            FROM tlias_token_usage
            """)
    @Results({
        @Result(property = "totalTokens", column = "totalTokens"),
        @Result(property = "totalPromptTokens", column = "totalPromptTokens"),
        @Result(property = "totalCompletionTokens", column = "totalCompletionTokens"),
        @Result(property = "totalSessions", column = "totalSessions"),
        @Result(property = "totalUsers", column = "totalUsers")
    })
    Map<String, Object> selectOverview();
}
