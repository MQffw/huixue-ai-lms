package com.itheima.service.impl;

import com.itheima.ai.cache.AiAnswerCache;
import com.itheima.mapper.CourseMapper;
import com.itheima.mapper.CourseScheduleMapper;
import com.itheima.pojo.Course;
import com.itheima.pojo.PageResult;
import com.itheima.service.CourseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class CourseServiceImpl implements CourseService {

    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private CourseScheduleMapper courseScheduleMapper;
    @Autowired
    private AiAnswerCache aiAnswerCache;

    @Override
    public PageResult<Course> page(String name, Integer subject, int page, int pageSize) {
        long total = courseMapper.count(name, subject);
        int start = (page - 1) * pageSize;
        List<Course> rows = courseMapper.pageList(name, subject, start, pageSize);
        return new PageResult<>(total, rows);
    }

    @Override
    public Course getById(Integer id) {
        return courseMapper.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Course course) {
        courseMapper.insert(course);
        aiAnswerCache.clear();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Course course) {
        courseMapper.update(course);
        aiAnswerCache.clear();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Integer id) {
        courseScheduleMapper.deleteByCourseId(id);  // 级联删除排课
        courseMapper.deleteById(id);
        aiAnswerCache.clear();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Integer> ids) {
        for (Integer id : ids) {
            courseScheduleMapper.deleteByCourseId(id);  // 级联删除排课
        }
        courseMapper.deleteByIds(ids);
        aiAnswerCache.clear();
    }

    @Override
    public List<Course> listAll() {
        return courseMapper.findAll();
    }

    @Override
    public List<Course> findBySubject(Integer subject) {
        return courseMapper.findBySubject(subject);
    }
}
