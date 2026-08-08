package com.itheima.mapper;

import com.itheima.pojo.TokenUsage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TokenUsageMapper {

    @Insert("INSERT INTO tlias_token_usage (user_id, model_name, prompt_tokens, completion_tokens, total_tokens, session_id, record_time) " +
            "VALUES (#{userId}, #{modelName}, #{promptTokens}, #{completionTokens}, #{totalTokens}, #{sessionId}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TokenUsage usage);

    @Select("SELECT id, user_id, model_name, prompt_tokens, completion_tokens, total_tokens, session_id, record_time " +
            "FROM tlias_token_usage WHERE user_id = #{userId} ORDER BY record_time DESC LIMIT #{limit}")
    List<TokenUsage> findByUserId(@Param("userId") Integer userId, @Param("limit") int limit);

    @Select("SELECT user_id, model_name, SUM(prompt_tokens) AS prompt_tokens, SUM(completion_tokens) AS completion_tokens, " +
            "SUM(total_tokens) AS total_tokens FROM tlias_token_usage " +
            "WHERE user_id = #{userId} AND record_time >= #{startTime} " +
            "GROUP BY user_id, model_name")
    List<TokenUsage> sumByUserIdSince(@Param("userId") Integer userId, @Param("startTime") String startTime);
}
