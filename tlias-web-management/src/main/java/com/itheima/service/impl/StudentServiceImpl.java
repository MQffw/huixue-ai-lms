package com.itheima.service.impl;

import com.itheima.ai.cache.AiAnswerCache;
import com.itheima.mapper.StudentMapper;
import com.itheima.pojo.Student;
import com.itheima.pojo.StudentQueryParam;
import com.itheima.pojo.PageResult;
import com.itheima.service.StudentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class StudentServiceImpl implements StudentService {
    
    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private AiAnswerCache aiAnswerCache;
    
    @Override
    public PageResult<Student> page(StudentQueryParam param) {
        Long count = studentMapper.count(param);
        List<Student> rows = studentMapper.pageList(param);
        return new PageResult<>(count, rows);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Integer> ids) {
        try {
            int count = studentMapper.deleteByIds(ids);
            log.info("批量删除学员成功: count={}", count);
            aiAnswerCache.clear();
        } catch (Exception e) {
            log.error("批量删除学员失败: count={}", ids.size(), e);
            throw e;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Student student) {
        try {
            student.setCreateTime(LocalDateTime.now());
            student.setUpdateTime(LocalDateTime.now());
            studentMapper.insert(student);
            log.info("新增学员成功: id={}, name={}", student.getId(), student.getName());
            aiAnswerCache.clear();
        } catch (Exception e) {
            log.error("新增学员失败: {}", student.getName(), e);
            throw e;
        }
    }
    
    @Override
    public Student getById(Integer id) {
        return studentMapper.getById(id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Student student) {
        try {
            student.setUpdateTime(LocalDateTime.now());
            studentMapper.updateById(student);
            log.info("修改学员成功: id={}, name={}", student.getId(), student.getName());
            aiAnswerCache.clear();
        } catch (Exception e) {
            log.error("修改学员失败: id={}", student.getId(), e);
            throw e;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateViolation(Integer id, Integer score) {
        try {
            studentMapper.updateViolation(id, score);
            log.info("学员违纪处理成功: id={}, score={}", id, score);
            aiAnswerCache.clear();
        } catch (Exception e) {
            log.error("学员违纪处理失败: id={}, score={}", id, score, e);
            throw e;
        }
    }

    @Override
    public List<Student> listAll() {
        return studentMapper.findAll();
    }
}
