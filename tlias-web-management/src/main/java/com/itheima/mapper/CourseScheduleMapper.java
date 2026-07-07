package com.itheima.mapper;

import com.itheima.pojo.CourseSchedule;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CourseScheduleMapper {

    @Select("""
        SELECT cs.id, cs.clazz_id, cs.course_id, cs.teacher_id, cs.class_date,
               cs.start_time, cs.end_time, cs.room, cs.create_time,
               c.name AS clazz_name, co.name AS course_name, e.name AS teacher_name
        FROM course_schedule cs
        LEFT JOIN clazz c ON cs.clazz_id = c.id
        LEFT JOIN course co ON cs.course_id = co.id
        LEFT JOIN emp e ON cs.teacher_id = e.id
        WHERE cs.class_date = #{date}
        ORDER BY cs.clazz_id, cs.start_time
        """)
    List<CourseSchedule> findByDate(LocalDate date);

    @Select("""
        SELECT cs.id, cs.clazz_id, cs.course_id, cs.teacher_id, cs.class_date,
               cs.start_time, cs.end_time, cs.room, cs.create_time,
               c.name AS clazz_name, co.name AS course_name, e.name AS teacher_name
        FROM course_schedule cs
        LEFT JOIN clazz c ON cs.clazz_id = c.id
        LEFT JOIN course co ON cs.course_id = co.id
        LEFT JOIN emp e ON cs.teacher_id = e.id
        WHERE cs.clazz_id = #{clazzId} AND cs.class_date = #{date}
        ORDER BY cs.start_time
        """)
    List<CourseSchedule> findByClazzIdAndDate(@Param("clazzId") Integer clazzId, @Param("date") LocalDate date);

    @Select("""
        SELECT cs.id, cs.clazz_id, cs.course_id, cs.teacher_id, cs.class_date,
               cs.start_time, cs.end_time, cs.room, cs.create_time,
               c.name AS clazz_name, co.name AS course_name, e.name AS teacher_name
        FROM course_schedule cs
        LEFT JOIN clazz c ON cs.clazz_id = c.id
        LEFT JOIN course co ON cs.course_id = co.id
        LEFT JOIN emp e ON cs.teacher_id = e.id
        WHERE cs.clazz_id = #{clazzId}
        ORDER BY cs.class_date, cs.start_time
        """)
    List<CourseSchedule> findByClazzId(Integer clazzId);

    // ===== 级联删除 =====
    @Delete("DELETE FROM course_schedule WHERE course_id = #{courseId}")
    int deleteByCourseId(Integer courseId);

    @Delete("DELETE FROM course_schedule WHERE clazz_id = #{clazzId}")
    int deleteByClazzId(Integer clazzId);

    // ===== CRUD =====
    @Insert("INSERT INTO course_schedule (clazz_id, course_id, teacher_id, class_date, start_time, end_time, room, create_time) " +
            "VALUES (#{clazzId}, #{courseId}, #{teacherId}, #{classDate}, #{startTime}, #{endTime}, #{room}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CourseSchedule schedule);

    @Update("UPDATE course_schedule SET clazz_id=#{clazzId}, course_id=#{courseId}, teacher_id=#{teacherId}, " +
            "class_date=#{classDate}, start_time=#{startTime}, end_time=#{endTime}, room=#{room} WHERE id=#{id}")
    int update(CourseSchedule schedule);

    @Delete("DELETE FROM course_schedule WHERE id = #{id}")
    int deleteById(Integer id);
}
