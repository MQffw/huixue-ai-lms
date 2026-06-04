package com.itheima.controller;

import com.itheima.pojo.PageResult;
import com.itheima.pojo.Result;
import com.itheima.pojo.Student;
import com.itheima.pojo.StudentQueryParam;
import com.itheima.service.StudentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequestMapping("/students")
@RestController
public class StudentController {
    @Autowired
    private StudentService studentService;

    // 学员分页条件查询
    @GetMapping
    public Result page(StudentQueryParam studentQueryParam) {
        log.info("学员分页查询：{}", studentQueryParam);
        PageResult<Student> pageResult = studentService.page(studentQueryParam);
        return Result.success(pageResult);
    }

    // 批量删除学员
    @DeleteMapping("/{ids}")
    public Result delete(@PathVariable String ids) {
        log.info("批量删除学员：{}", ids);
        List<Integer> idList = Arrays.stream(ids.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        studentService.deleteByIds(idList);
        return Result.success();
    }

    // 新增学员
    @PostMapping
    public Result add(@RequestBody Student student) {
        log.info("新增学员：{}", student);
        studentService.add(student);
        return Result.success();
    }

    // 根据ID查询学员
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        log.info("根据ID查询学员：{}", id);
        Student student = studentService.getById(id);
        return Result.success(student);
    }

    // 修改学员
    @PutMapping
    public Result update(@RequestBody Student student) {
        log.info("修改学员：{}", student);
        studentService.update(student);
        return Result.success();
    }

    // 违纪处理
    @PutMapping("/violation/{id}/{score}")
    public Result updateViolation(@PathVariable Integer id, @PathVariable Integer score) {
        log.info("学员违纪处理：学员ID={}, 扣分={}", id, score);
        studentService.updateViolation(id, score);
        return Result.success();
    }
}