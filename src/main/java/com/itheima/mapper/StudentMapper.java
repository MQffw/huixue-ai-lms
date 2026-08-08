package com.itheima.mapper;

import com.itheima.pojo.Student;
import com.itheima.pojo.StudentQueryParam;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface StudentMapper {
    
    List<Student> pageList(StudentQueryParam param);
    Long count(StudentQueryParam param);
    int deleteByIds(@Param("ids") List<Integer> ids);
    
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Student student);
    
    Student getById(Integer id);
    int updateById(Student student);
    int updateViolation(@Param("id") Integer id, @Param("score") Integer score);
    int incrementViolation(@Param("id") Integer id, @Param("score") Integer score);
    
    List<Map<String, Object>> countStudentDegreeData();
    List<Map<String, Object>> countStudentClazzData();
    List<Map<String, Object>> countStudentGenderData();

    @Select("SELECT s.*, c.name AS clazz_name FROM student s LEFT JOIN clazz c ON s.clazz_id = c.id ORDER BY s.id")
    @Results({
        @Result(property = "clazzName", column = "clazz_name")
    })
    List<Student> findAll();
}
