package com.itheima.controller;

import com.itheima.ai.cache.AiAnswerCache;
import com.itheima.mapper.EmploymentMapper;
import com.itheima.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RequestMapping("/employments")
@RestController
public class EmploymentController {

    @Autowired private EmploymentMapper employmentMapper;
    @Autowired private AiAnswerCache aiAnswerCache;

    /** 分页+搜索 */
    @GetMapping
    public Result page(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) Integer clazzId,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false) String studentName) {
        List<Employment> all;
        if (clazzId != null) {
            all = employmentMapper.findByClazzId(clazzId);
        } else {
            all = employmentMapper.findAll();
        }
        if (status != null) all = all.stream().filter(e -> e.getStatus() != null && e.getStatus().equals(status)).toList();
        if (studentName != null && !studentName.isEmpty())
            all = all.stream().filter(e -> e.getStudentName() != null && e.getStudentName().contains(studentName)).toList();
        all.sort((a, b) -> {
            if (a.getSalary() == null && b.getSalary() == null) return 0;
            if (a.getSalary() == null) return 1;
            if (b.getSalary() == null) return -1;
            return b.getSalary().compareTo(a.getSalary());
        });
        long total = all.size();
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, all.size());
        return Result.success(new PageResult<>(total, start < total ? all.subList(start, end) : List.of()));
    }

    @GetMapping("/student/{studentId}")
    public Result getByStudent(@PathVariable Integer studentId) {
        Employment e = employmentMapper.findByStudentId(studentId);
        return e != null ? Result.success(e) : Result.error(404, "暂无就业记录");
    }

    @GetMapping("/class/{clazzId}/stats")
    public Result classStats(@PathVariable Integer clazzId) { return Result.success(employmentMapper.getEmploymentStats(clazzId)); }

    @GetMapping("/stats")
    public Result allStats() { return Result.success(employmentMapper.getEmploymentStatsByClazz()); }

    @PostMapping
    public Result add(@RequestBody Employment e) {
        if (e.getStudentId() == null) return Result.error(400, "学员ID不能为空");
        employmentMapper.insert(e);
        aiAnswerCache.clear();
        return Result.success();
    }

    @PutMapping
    public Result update(@RequestBody Employment e) {
        if (e.getId() == null) return Result.error(400, "ID不能为空");
        employmentMapper.update(e);
        aiAnswerCache.clear();
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        employmentMapper.deleteById(id);
        aiAnswerCache.clear();
        return Result.success();
    }

    @DeleteMapping("/batch/{ids}")
    public Result deleteBatch(@PathVariable String ids) {
        for (String id : ids.split(",")) employmentMapper.deleteById(Integer.parseInt(id));
        aiAnswerCache.clear();
        return Result.success();
    }
}
