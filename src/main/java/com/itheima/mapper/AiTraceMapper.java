package com.itheima.mapper;

import org.apache.ibatis.annotations.*;

import java.util.Map;

/**
 * AI 全链路追踪表 Mapper（tlias_ai_trace）
 */
@Mapper
public interface AiTraceMapper {

    @Insert("""
            INSERT INTO tlias_ai_trace (trace_id, user_id, session_id, question, intent,
                model_type, fallback, tool_calls, rag_hit, answer_chars, latency_ms,
                status, error_msg, detail_json, create_time)
            VALUES (#{traceId}, #{userId}, #{sessionId}, #{question}, #{intent},
                #{modelType}, #{fallback}, #{toolCalls}, #{ragHit}, #{answerChars}, #{latencyMs},
                #{status}, #{errorMsg}, CAST(#{detailJson} AS JSON), NOW())
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

    @Select("SELECT * FROM tlias_ai_trace WHERE trace_id = #{traceId}")
    Map<String, Object> findByTraceId(String traceId);
}
