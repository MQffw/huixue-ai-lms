package com.huixuetong.controller;

import com.huixuetong.pojo.Course;
import com.huixuetong.pojo.PageResult;
import com.huixuetong.pojo.Result;
import com.huixuetong.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RequestMapping("/courses")
@RestController
@Tag(name = "课程管理", description = "课程的增删改查接口")
public class CourseController {

    @Autowired
    private CourseService courseService;

    /** 分页+搜索 */
    @Operation(summary = "课程分页查询")
    @GetMapping
    public Result page(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) String name,
                       @RequestParam(required = false) Integer subject) {
        PageResult<Course> result = courseService.page(name, subject, page, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "查询所有课程")
    @GetMapping("/list")
    public Result listAll() { return Result.success(courseService.listAll()); }

    @Operation(summary = "查询课程详情")
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        Course c = courseService.getById(id);
        return c != null ? Result.success(c) : Result.error(404, "课程不存在");
    }

    @Operation(summary = "按科目查询课程")
    @GetMapping("/subject/{subject}")
    public Result getBySubject(@PathVariable Integer subject) {
        return Result.success(courseService.findBySubject(subject));
    }

    @Operation(summary = "新增课程")
    @PostMapping
    public Result add(@RequestBody Course course) {
        if (course.getName() == null || course.getName().trim().isEmpty())
            return Result.error(400, "课程名称不能为空");
        courseService.add(course);
        return Result.success();
    }

    @Operation(summary = "修改课程")
    @PutMapping
    public Result update(@RequestBody Course course) {
        if (course.getId() == null) return Result.error(400, "ID不能为空");
        courseService.update(course);
        return Result.success();
    }

    @Operation(summary = "删除课程")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        courseService.deleteById(id);
        return Result.success();
    }

    @Operation(summary = "批量删除课程")
    @DeleteMapping("/batch/{ids}")
    public Result deleteBatch(@PathVariable String ids) {
        List<Integer> idList = Arrays.stream(ids.split(",")).map(Integer::parseInt).toList();
        courseService.deleteByIds(idList);
        return Result.success();
    }
}