package com.itheima.mapper;

import com.itheima.pojo.ChatRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI 对话记录 Mapper
 */
@Mapper
public interface ChatRecordMapper {

    @Insert("INSERT INTO tlias_ai_chat_record (user_id, session_id, user_message, ai_answer, tool_calls, create_time) " +
            "VALUES (#{userId}, #{sessionId}, #{userMessage}, #{aiAnswer}, #{toolCalls}, #{createTime})")
    void insert(ChatRecord record);

    @Select("SELECT id, user_id, session_id, user_message, ai_answer, tool_calls, create_time " +
            "FROM tlias_ai_chat_record WHERE user_id = #{userId} AND session_id = #{sessionId} " +
            "ORDER BY create_time DESC LIMIT #{limit}")
    List<ChatRecord> findBySession(Integer userId, String sessionId, int limit);
}
