package com.itheima.mapper;

import com.itheima.pojo.Course;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CourseMapper {

    // ===== 分页+搜索 =====
    @Select("""
        <script>
        SELECT id, name, subject, hours, description, create_time FROM course
        <where>
            <if test='name != null and name != ""'>AND name LIKE CONCAT('%',#{name},'%')</if>
            <if test='subject != null'>AND subject = #{subject}</if>
        </where>
        ORDER BY subject, id LIMIT #{start}, #{pageSize}
        </script>
        """)
    List<Course> pageList(@Param("name") String name, @Param("subject") Integer subject,
                          @Param("start") int start, @Param("pageSize") int pageSize);

    @Select("""
        <script>
        SELECT COUNT(*) FROM course
        <where>
            <if test='name != null and name != ""'>AND name LIKE CONCAT('%',#{name},'%')</if>
            <if test='subject != null'>AND subject = #{subject}</if>
        </where>
        </script>
        """)
    long count(@Param("name") String name, @Param("subject") Integer subject);

    // ===== 基础CRUD =====
    @Select("SELECT id, name, subject, hours, description, create_time, update_time FROM course ORDER BY subject, id")
    List<Course> findAll();

    @Select("SELECT id, name, subject, hours, description, create_time, update_time FROM course WHERE id = #{id}")
    Course getById(Integer id);

    @Select("SELECT id, name, subject, hours, description, create_time, update_time FROM course WHERE subject = #{subject} ORDER BY id")
    List<Course> findBySubject(Integer subject);

    @Insert("INSERT INTO course (name, subject, hours, description, create_time) VALUES (#{name}, #{subject}, #{hours}, #{description}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Course course);

    @Update("UPDATE course SET name=#{name}, subject=#{subject}, hours=#{hours}, description=#{description} WHERE id=#{id}")
    int update(Course course);

    @Delete("DELETE FROM course WHERE id = #{id}")
    int deleteById(Integer id);

    @Delete("<script>DELETE FROM course WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int deleteByIds(@Param("ids") List<Integer> ids);
}
