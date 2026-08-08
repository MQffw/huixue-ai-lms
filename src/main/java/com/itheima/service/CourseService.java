package com.itheima.service;

import com.itheima.pojo.Course;
import com.itheima.pojo.PageResult;

import java.util.List;

public interface CourseService {
    PageResult<Course> page(String name, Integer subject, int page, int pageSize);
    Course getById(Integer id);
    void add(Course course);
    void update(Course course);
    void deleteById(Integer id);
    void deleteByIds(List<Integer> ids);
    List<Course> listAll();
    List<Course> findBySubject(Integer subject);
}
