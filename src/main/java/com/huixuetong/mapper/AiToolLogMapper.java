package com.huixuetong.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 工具调用日志（hxt_tool_call_log）
 */
@Mapper
public interface AiToolLogMapper {

    @Insert("""
            INSERT INTO hxt_tool_call_log
                (user_id, session_id, tool_name, tool_args, success, duration_ms, call_time)
            VALUES
                (#{userId}, #{sessionId}, #{toolName}, #{toolArgs}, #{success}, #{durationMs}, NOW())
            """)
    int insert(@Param("userId") Integer userId,
               @Param("sessionId") String sessionId,
               @Param("toolName") String toolName,
               @Param("toolArgs") String toolArgs,
               @Param("success") boolean success,
               @Param("durationMs") int durationMs);
}
