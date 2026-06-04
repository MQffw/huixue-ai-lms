package com.itheima.mapper;

import com.itheima.pojo.Student;
import com.itheima.pojo.StudentQueryParam;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface StudentMapper {
    List<Student> pageList(StudentQueryParam param);

    Long count(StudentQueryParam param);

    void deleteByIds(List<Integer> ids);

    void insert(Student student);

    Student getById(Integer id);

    void updateById(Student student);

    void updateViolation(Integer id, Integer score);

    List<Map<String, Object>> countStudentDegreeData();

    List<Map<String, Object>> countStudentClazzData();
}