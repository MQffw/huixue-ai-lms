package com.itheima.mapper;

import com.itheima.pojo.Notice;
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

    @Update("UPDATE notice SET title=#{title}, content=#{content}, type=#{type}, target_audience=#{targetAudience}, " +
            "publish_emp_id=#{publishEmpId}, is_top=#{isTop} WHERE id=#{id}")
    int update(Notice n);

    @Delete("DELETE FROM notice WHERE id = #{id}")
    int deleteById(Integer id);

    @Delete("<script>DELETE FROM notice WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int deleteByIds(@Param("ids") List<Integer> ids);
}
