package com.huixuetong.controller;

import com.huixuetong.pojo.PageResult;
import com.huixuetong.pojo.Result;
import com.huixuetong.pojo.Student;
import com.huixuetong.pojo.StudentQueryParam;
import com.huixuetong.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 学员管理控制器
 */
@Slf4j
@RequestMapping("/students")
@RestController
@Validated
@Tag(name = "学员管理", description = "学员的增删改查接口")
public class StudentController {
    
    @Autowired
    private StudentService studentService;
    
    @Operation(summary = "学员分页查询")
    @GetMapping
    public Result page(StudentQueryParam studentQueryParam) {
        log.info("学员分页查询: page={}, pageSize={}", 
                studentQueryParam.getPage(), studentQueryParam.getPageSize());
        PageResult<Student> pageResult = studentService.page(studentQueryParam);
        return Result.success(pageResult);
    }
    
    @Operation(summary = "批量删除学员")
    @DeleteMapping("/{ids}")
    public Result delete(@PathVariable String ids) {
        log.info("批量删除学员: count={}", ids.split(",").length);
        List<Integer> idList = Arrays.stream(ids.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        studentService.deleteByIds(idList);
        return Result.success();
    }
    
    @Operation(summary = "新增学员")
    @PostMapping
    public Result add(@RequestBody Student student) {
        if (student.getName() == null || student.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("学员姓名不能为空");
        }
        log.info("新增学员: name={}", student.getName());
        studentService.add(student);
        return Result.success();
    }
    
    @Operation(summary = "查询所有学员")
    @GetMapping("/list")
    public Result listAll() {
        List<Student> students = studentService.listAll();
        return Result.success(students);
    }

    @Operation(summary = "查询学员详情")
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        log.info("查询学员: id={}", id);
        Student student = studentService.getById(id);
        return Result.success(student);
    }
    
    @Operation(summary = "修改学员")
    @PutMapping
    public Result update(@RequestBody Student student) {
        if (student.getName() == null || student.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("学员姓名不能为空");
        }
        log.info("修改学员: id={}, name={}", student.getId(), student.getName());
        studentService.update(student);
        return Result.success();
    }
    
    @Operation(summary = "学员违纪处理")
    @PutMapping("/violation/{id}/{score}")
    public Result updateViolation(@PathVariable Integer id, @PathVariable Integer score) {
        log.info("学员违纪处理: id={}, score={}", id, score);
        studentService.updateViolation(id, score);
        return Result.success();
    }
}