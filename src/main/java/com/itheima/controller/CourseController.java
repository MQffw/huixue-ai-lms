package com.itheima.controller;

import com.itheima.pojo.Course;
import com.itheima.pojo.PageResult;
import com.itheima.pojo.Result;
import com.itheima.service.CourseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RequestMapping("/courses")
@RestController
public class CourseController {

    @Autowired
    private CourseService courseService;

    /** 分页+搜索 */
    @GetMapping
    public Result page(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) String name,
                       @RequestParam(required = false) Integer subject) {
        PageResult<Course> result = courseService.page(name, subject, page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/list")
    public Result listAll() { return Result.success(courseService.listAll()); }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        Course c = courseService.getById(id);
        return c != null ? Result.success(c) : Result.error(404, "课程不存在");
    }

    @GetMapping("/subject/{subject}")
    public Result getBySubject(@PathVariable Integer subject) {
        return Result.success(courseService.findBySubject(subject));
    }

    @PostMapping
    public Result add(@RequestBody Course course) {
        if (course.getName() == null || course.getName().trim().isEmpty())
            return Result.error(400, "课程名称不能为空");
        courseService.add(course);
        return Result.success();
    }

    @PutMapping
    public Result update(@RequestBody Course course) {
        if (course.getId() == null) return Result.error(400, "ID不能为空");
        courseService.update(course);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        courseService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/batch/{ids}")
    public Result deleteBatch(@PathVariable String ids) {
        List<Integer> idList = Arrays.stream(ids.split(",")).map(Integer::parseInt).toList();
        courseService.deleteByIds(idList);
        return Result.success();
    }
}
