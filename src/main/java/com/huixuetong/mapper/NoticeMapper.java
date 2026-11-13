package com.huixuetong.mapper;

import com.huixuetong.pojo.Notice;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface NoticeMapper {

    @Select("""
        SELECT n.id, n.title, n.content, n.type, n.target_audience,
               n.publish_emp_id, n.publish_time, n.is_top, n.create_time,
               e.name AS publisher_name
        FROM notice n
        LEFT JOIN emp e ON n.publish_emp_id = e.id
        ORDER BY n.is_top DESC, n.publish_time DESC
        LIMIT #{limit}
        """)
    List<Notice> findLatest(@Param("limit") int limit);

    @Select("""
        SELECT n.id, n.title, n.content, n.type, n.target_audience,
               n.publish_emp_id, n.publish_time, n.is_top, n.create_time,
               e.name AS publisher_name
        FROM notice n
        LEFT JOIN emp e ON n.publish_emp_id = e.id
        WHERE n.type = #{type}
        ORDER BY n.is_top DESC, n.publish_time DESC
        """)
    List<Notice> findByType(Integer type);

    @Select("""
        SELECT n.id, n.title, n.content, n.type, n.target_audience,
               n.publish_emp_id, n.publish_time, n.is_top, n.create_time,
               e.name AS publisher_name
        FROM notice n
        LEFT JOIN emp e ON n.publish_emp_id = e.id
        WHERE n.id = #{id}
        """)
    Notice getById(Integer id);

    @Select("""
        SELECT n.id, n.title, n.content, n.type, n.target_audience,
               n.publish_emp_id, n.publish_time, n.is_top, n.create_time,
               e.name AS publisher_name
        FROM notice n
        LEFT JOIN emp e ON n.publish_emp_id = e.id
        WHERE n.title LIKE CONCAT('%',#{keyword},'%') OR n.content LIKE CONCAT('%',#{keyword},'%')
        ORDER BY n.is_top DESC, n.publish_time DESC
        """)
    List<Notice> searchByKeyword(String keyword);

    @Insert("INSERT INTO notice (title, content, type, target_audience, publish_emp_id, publish_time, is_top, create_time) " +
            "VALUES (#{title}, #{content}, #{type}, #{targetAudience}, #{publishEmpId}, NOW(), #{isTop}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Notice n);

    /** SQL 层分页 + 定向过滤（targetAudience 过滤时同时包含“全体”与未设置的公告） */
    @Select("""
            <script>
            SELECT n.id, n.title, n.content, n.type, n.target_audience,
                   n.publish_emp_id, n.publish_time, n.is_top, n.create_time,
                   e.name AS publisher_name
            FROM notice n
            LEFT JOIN emp e ON n.publish_emp_id = e.id
            <where>
                <if test="keyword != null and keyword != ''">
                    AND (n.title LIKE CONCAT('%', #{keyword}, '%') OR n.content LIKE CONCAT('%', #{keyword}, '%'))
                </if>
                <if test="type != null"> AND n.type = #{type} </if>
                <if test="audience != null and audience != ''">
                    AND (n.target_audience = #{audience} OR n.target_audience = '全体' OR n.target_audience IS NULL)
                </if>
            </where>
            ORDER BY n.is_top DESC, n.publish_time DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Notice> findPage(@Param("keyword") String keyword,
                          @Param("type") Integer type,
                          @Param("audience") String audience,
                          @Param("offset") int offset,
                          @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(*) FROM notice n
            <where>
                <if test="keyword != null and keyword != ''">
                    AND (n.title LIKE CONCAT('%', #{keyword}, '%') OR n.content LIKE CONCAT('%', #{keyword}, '%'))
                </if>
                <if test="type != null"> AND n.type = #{type} </if>
                <if test="audience != null and audience != ''">
                    AND (n.target_audience = #{audience} OR n.target_audience = '全体' OR n.target_audience IS NULL)
                </if>
            </where>
            </script>
            """)
    long countPage(@Param("keyword") String keyword,
                   @Param("type") Integer type,
                   @Param("audience") String audience);

    @Update("UPDATE notice SET title=#{title}, content=#{content}, type=#{type}, target_audience=#{targetAudience}, " +
            "publish_emp_id=#{publishEmpId}, is_top=#{isTop} WHERE id=#{id}")
    int update(Notice n);

    @Delete("DELETE FROM notice WHERE id = #{id}")
    int deleteById(Integer id);

    @Delete("<script>DELETE FROM notice WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int deleteByIds(@Param("ids") List<Integer> ids);
}
