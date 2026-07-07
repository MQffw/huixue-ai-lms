package com.itheima.mapper;

import com.itheima.pojo.Exam;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ExamMapper {

    @Select("""
        SELECT e.id, e.name, e.clazz_id, e.course_id, e.exam_date, e.full_score,
               e.pass_score, e.create_time, c.name AS clazz_name, co.name AS course_name
        FROM exam e
        LEFT JOIN clazz c ON e.clazz_id = c.id
        LEFT JOIN course co ON e.course_id = co.id
        ORDER BY e.exam_date DESC
        """)
    List<Exam> findAll();

    @Select("""
        SELECT e.id, e.name, e.clazz_id, e.course_id, e.exam_date, e.full_score,
               e.pass_score, e.create_time, c.name AS clazz_name, co.name AS course_name
        FROM exam e
        LEFT JOIN clazz c ON e.clazz_id = c.id
        LEFT JOIN course co ON e.course_id = co.id
        WHERE e.id = #{id}
        """)
    Exam getById(Integer id);

    @Select("""
        SELECT e.id, e.name, e.clazz_id, e.course_id, e.exam_date, e.full_score,
               e.pass_score, e.create_time, c.name AS clazz_name, co.name AS course_name
        FROM exam e
        LEFT JOIN clazz c ON e.clazz_id = c.id
        LEFT JOIN course co ON e.course_id = co.id
        WHERE e.clazz_id = #{clazzId}
        ORDER BY e.exam_date DESC
        """)
    List<Exam> findByClazzId(Integer clazzId);
}
