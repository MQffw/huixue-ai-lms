package com.huixuetong.service;

import com.huixuetong.pojo.PageResult;
import com.huixuetong.pojo.Student;
import com.huixuetong.pojo.StudentQueryParam;

import java.util.List;

public interface StudentService {
    PageResult<Student> page(StudentQueryParam param);

    void deleteByIds(List<Integer> ids);

    void add(Student student);

    Student getById(Integer id);

    void update(Student student);

    void updateViolation(Integer id, Integer score);

    List<Student> listAll();
}