package com.huixuetong.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * AI 全链路追踪表 Mapper（hxt_ai_trace）
 */
@Mapper
public interface AiTraceMapper {

    @Insert("""
            INSERT INTO hxt_ai_trace (trace_id, user_id, session_id, question, intent,
                model_type, fallback, tool_calls, rag_hit, answer_chars, latency_ms,
                status, error_msg, detail_json, create_time)
            VALUES (#{traceId}, #{userId}, #{sessionId}, #{question}, #{intent},
                #{modelType}, #{fallback}, #{toolCalls}, #{ragHit}, #{answerChars}, #{latencyMs},
                #{status}, #{errorMsg}, #{detailJson}, NOW())
            """)
    int insert(@Param("traceId") String traceId,
               @Param("userId") Integer userId,
               @Param("sessionId") String sessionId,
               @Param("question") String question,
               @Param("intent") String intent,
               @Param("modelType") String modelType,
               @Param("fallback") boolean fallback,
               @Param("toolCalls") int toolCalls,
               @Param("ragHit") boolean ragHit,
               @Param("answerChars") int answerChars,
               @Param("latencyMs") int latencyMs,
               @Param("status") String status,
               @Param("errorMsg") String errorMsg,
               @Param("detailJson") String detailJson);

    @Select("""
            SELECT trace_id, user_id, session_id, question, intent, model_type, fallback,
                   tool_calls, rag_hit, answer_chars, latency_ms, status, error_msg, detail_json,
                   DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') AS create_time
            FROM hxt_ai_trace WHERE trace_id = #{traceId}
            """)
    Map<String, Object> findByTraceId(String traceId);

    @Select("""
            <script>
            SELECT id, trace_id AS traceId, user_id AS userId, session_id AS sessionId,
                   question, intent, model_type AS modelType, fallback,
                   tool_calls AS toolCalls, rag_hit AS ragHit, answer_chars AS answerChars,
                   latency_ms AS latencyMs, status, error_msg AS errorMsg,
                   DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') AS createTime
            FROM hxt_ai_trace
            <where>
                <if test="intent != null and intent != ''"> AND intent = #{intent} </if>
                <if test="status != null and status != ''"> AND status = #{status} </if>
                <if test="keyword != null and keyword != ''"> AND question LIKE CONCAT('%', #{keyword}, '%') </if>
            </where>
            ORDER BY id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Map<String, Object>> findPage(@Param("intent") String intent,
                                       @Param("status") String status,
                                       @Param("keyword") String keyword,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(*) FROM hxt_ai_trace
            <where>
                <if test="intent != null and intent != ''"> AND intent = #{intent} </if>
                <if test="status != null and status != ''"> AND status = #{status} </if>
                <if test="keyword != null and keyword != ''"> AND question LIKE CONCAT('%', #{keyword}, '%') </if>
            </where>
            </script>
            """)
    long countPage(@Param("intent") String intent,
                   @Param("status") String status,
                   @Param("keyword") String keyword);

    @Select("""
            SELECT tool_name AS toolName, tool_args AS toolArgs, success,
                   duration_ms AS durationMs,
                   DATE_FORMAT(call_time, '%Y-%m-%d %H:%i:%s') AS callTime
            FROM hxt_tool_call_log
            WHERE user_id = #{userId} AND session_id = #{sessionId}
            ORDER BY id DESC LIMIT 20
            """)
    List<Map<String, Object>> findToolLogs(@Param("userId") Integer userId,
                                           @Param("sessionId") String sessionId);
}
