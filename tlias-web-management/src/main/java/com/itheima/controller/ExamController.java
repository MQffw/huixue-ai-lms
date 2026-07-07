package com.itheima.controller;

import com.itheima.mapper.ExamMapper;
import com.itheima.pojo.Exam;
import com.itheima.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequestMapping("/exams")
@RestController
public class ExamController {

    @Autowired
    private ExamMapper examMapper;

    @GetMapping
    public Result listAll() {
        List<Exam> list = examMapper.findAll();
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        Exam exam = examMapper.getById(id);
        return exam != null ? Result.success(exam) : Result.error(404, "考试不存在");
    }

    @GetMapping("/clazz/{clazzId}")
    public Result getByClazz(@PathVariable Integer clazzId) {
        List<Exam> list = examMapper.findByClazzId(clazzId);
        return Result.success(list);
    }
}
