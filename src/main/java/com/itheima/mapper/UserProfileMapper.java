package com.itheima.mapper;

import org.apache.ibatis.annotations.*;

import java.util.Map;
import java.util.Optional;

/**
 * 用户 AI 画像 Mapper（tlias_user_ai_profile）
 */
@Mapper
public interface UserProfileMapper {

    @Select("SELECT user_id, favorite_tools, common_intents, tags, last_active_at " +
            "FROM tlias_user_ai_profile WHERE user_id = #{userId}")
    @Results({
        @Result(property = "userId", column = "user_id"),
        @Result(property = "favoriteTools", column = "favorite_tools"),
        @Result(property = "commonIntents", column = "common_intents"),
        @Result(property = "tags", column = "tags"),
        @Result(property = "lastActiveAt", column = "last_active_at")
    })
    Optional<Map<String, String>> findByUserId(Integer userId);

    /**
     * UPSERT（MySQL 方言 ON DUPLICATE KEY UPDATE）
     */
    @Insert("""
            INSERT INTO tlias_user_ai_profile (user_id, favorite_tools, common_intents, tags, last_active_at, create_time, update_time)
            VALUES (#{userId}, #{favoriteTools}, #{commonIntents}, #{tags}, #{lastActiveAt}, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
              favorite_tools = VALUES(favorite_tools),
              common_intents = VALUES(common_intents),
              tags = VALUES(tags),
              last_active_at = VALUES(last_active_at),
              update_time = NOW()
            """)
    int upsert(@Param("userId") Integer userId,
               @Param("favoriteTools") String favoriteTools,
               @Param("commonIntents") String commonIntents,
               @Param("tags") String tags,
               @Param("lastActiveAt") String lastActiveAt);
}
