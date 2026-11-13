package com.huixuetong.controller;

import com.huixuetong.ai.cache.AiAnswerCache;
import com.huixuetong.mapper.StudentMapper;
import com.huixuetong.mapper.ViolationLogMapper;
import com.huixuetong.pojo.*;
import com.huixuetong.utils.PageQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@RequestMapping("/violations")
@RestController
@Tag(name = "违纪管理", description = "违纪记录的增删改查接口")
public class ViolationController {

    @Autowired private ViolationLogMapper violationLogMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private AiAnswerCache aiAnswerCache;

    /** 分页+搜索 */
    @Operation(summary = "违纪记录分页查询")
    @GetMapping
    public Result page(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) String studentName,
                       @RequestParam(required = false) String violationType,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate) {
        String sd = (startDate != null) ? startDate : LocalDate.now().minusDays(365).toString();
        String ed = (endDate != null) ? endDate : LocalDate.now().toString();
        PageQuery q = PageQuery.of(page, pageSize);
        List<ViolationLog> rows = violationLogMapper.findPage(sd, ed, studentName, violationType, q.offset(), q.limit());
        long total = violationLogMapper.countPage(sd, ed, studentName, violationType);
        return Result.success(new PageResult<>(total, rows));
    }

    @Operation(summary = "查询学员违纪记录")
    @GetMapping("/student/{studentId}")
    public Result getByStudent(@PathVariable Integer studentId) { return Result.success(violationLogMapper.findByStudentId(studentId)); }

    @Operation(summary = "查询近期违纪记录")
    @GetMapping("/recent")
    public Result recent(@RequestParam(defaultValue = "30") int days) {
        LocalDate ed = LocalDate.now();
        return Result.success(violationLogMapper.findRecent(ed.minusDays(days).toString(), ed.toString()));
    }

    @Operation(summary = "新增违纪记录")
    @PostMapping
    public Result add(@RequestBody ViolationLog v) {
        if (v.getStudentId() == null) return Result.error(400, "学员ID不能为空");
        violationLogMapper.insert(v);
        // 联动更新学员表的违纪次数和扣分
        int score = v.getDeductScore() != null ? v.getDeductScore() : 0;
        studentMapper.incrementViolation(v.getStudentId(), score);
        aiAnswerCache.clear();
        return Result.success();
    }

    @Operation(summary = "修改违纪记录")
    @PutMapping
    public Result update(@RequestBody ViolationLog v) {
        if (v.getId() == null) return Result.error(400, "ID不能为空");
        violationLogMapper.update(v);
        aiAnswerCache.clear();
        return Result.success();
    }

    @Operation(summary = "删除违纪记录")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        violationLogMapper.deleteById(id);
        aiAnswerCache.clear();
        return Result.success();
    }

    @Operation(summary = "批量删除违纪记录")
    @DeleteMapping("/batch/{ids}")
    public Result deleteBatch(@PathVariable String ids) {
        for (String id : ids.split(",")) violationLogMapper.deleteById(Integer.parseInt(id));
        aiAnswerCache.clear();
        return Result.success();
    }
}