package com.huixuetong.controller;

import com.huixuetong.ai.cache.AiAnswerCache;
import com.huixuetong.mapper.EmploymentMapper;
import com.huixuetong.pojo.*;
import com.huixuetong.utils.PageQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RequestMapping("/employments")
@RestController
@Tag(name = "就业管理", description = "就业信息的增删改查接口")
public class EmploymentController {

    @Autowired private EmploymentMapper employmentMapper;
    @Autowired private AiAnswerCache aiAnswerCache;

    /** 分页+搜索 */
    @Operation(summary = "就业信息分页查询")
    @GetMapping
    public Result page(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) Integer clazzId,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false) String studentName) {
        PageQuery q = PageQuery.of(page, pageSize);
        List<Employment> rows = employmentMapper.findPage(clazzId, status, studentName, q.offset(), q.limit());
        long total = employmentMapper.countPage(clazzId, status, studentName);
        return Result.success(new PageResult<>(total, rows));
    }

    @Operation(summary = "查询学员就业记录")
    @GetMapping("/student/{studentId}")
    public Result getByStudent(@PathVariable Integer studentId) {
        Employment e = employmentMapper.findByStudentId(studentId);
        return e != null ? Result.success(e) : Result.error(404, "暂无就业记录");
    }

    @Operation(summary = "班级就业统计")
    @GetMapping("/class/{clazzId}/stats")
    public Result classStats(@PathVariable Integer clazzId) { return Result.success(employmentMapper.getEmploymentStats(clazzId)); }

    @Operation(summary = "就业总体统计")
    @GetMapping("/stats")
    public Result allStats() { return Result.success(employmentMapper.getEmploymentStatsByClazz()); }

    @Operation(summary = "新增就业记录")
    @PostMapping
    public Result add(@RequestBody Employment e) {
        if (e.getStudentId() == null) return Result.error(400, "学员ID不能为空");
        employmentMapper.insert(e);
        aiAnswerCache.clear();
        return Result.success();
    }

    @Operation(summary = "修改就业记录")
    @PutMapping
    public Result update(@RequestBody Employment e) {
        if (e.getId() == null) return Result.error(400, "ID不能为空");
        employmentMapper.update(e);
        aiAnswerCache.clear();
        return Result.success();
    }

    @Operation(summary = "删除就业记录")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        employmentMapper.deleteById(id);
        aiAnswerCache.clear();
        return Result.success();
    }

    @Operation(summary = "批量删除就业记录")
    @DeleteMapping("/batch/{ids}")
    public Result deleteBatch(@PathVariable String ids) {
        for (String id : ids.split(",")) employmentMapper.deleteById(Integer.parseInt(id));
        aiAnswerCache.clear();
        return Result.success();
    }
}