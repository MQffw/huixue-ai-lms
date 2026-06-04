package com.itheima.service.impl;

import com.itheima.mapper.StudentMapper;
import com.itheima.pojo.PageResult;
import com.itheima.pojo.Student;
import com.itheima.pojo.StudentQueryParam;
import com.itheima.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentMapper studentMapper;

    @Override
    public PageResult<Student> page(StudentQueryParam param) {
        //1. 查询总记录数
        Long total = studentMapper.count(param);
        //2. 计算分页起始位置 start
        Integer start = (param.getPage() - 1) * param.getPageSize();
        param.setStart(start);
        //3. 查询当前页数据列表
        List<Student> rows = studentMapper.pageList(param);
        //4. 封装分页结果返回
        return new PageResult<>(total, rows);
    }

    @Override
    public void deleteByIds(List<Integer> ids) {
        studentMapper.deleteByIds(ids);
    }

    @Override
    public void add(Student student) {
        studentMapper.insert(student);
    }

    @Override
    public Student getById(Integer id) {
        return studentMapper.getById(id);
    }

    @Override
    public void update(Student student) {
        studentMapper.updateById(student);
    }

    @Override
    public void updateViolation(Integer id, Integer score) {
        studentMapper.updateViolation(id, score);
    }
}