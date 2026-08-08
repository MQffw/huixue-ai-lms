package com.itheima.controller;

import com.itheima.ai.cache.AiAnswerCache;
import com.itheima.mapper.StudentMapper;
import com.itheima.mapper.ViolationLogMapper;
import com.itheima.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@RequestMapping("/violations")
@RestController
public class ViolationController {

    @Autowired private ViolationLogMapper violationLogMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private AiAnswerCache aiAnswerCache;

    /** 分页+搜索 */
    @GetMapping
    public Result page(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) String studentName,
                       @RequestParam(required = false) String violationType,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate) {
        LocalDate sd = (startDate != null) ? LocalDate.parse(startDate) : LocalDate.now().minusDays(90);
        LocalDate ed = (endDate != null) ? LocalDate.parse(endDate) : LocalDate.now();
        List<ViolationLog> all = violationLogMapper.findRecent(sd.toString(), ed.toString());

        if (studentName != null && !studentName.isEmpty())
            all = all.stream().filter(v -> v.getStudentName() != null && v.getStudentName().contains(studentName)).toList();
        if (violationType != null && !violationType.isEmpty())
            all = all.stream().filter(v -> v.getViolationType().equals(violationType)).toList();

        long total = all.size();
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, all.size());
        return Result.success(new PageResult<>(total, start < total ? all.subList(start, end) : List.of()));
    }

    @GetMapping("/student/{studentId}")
    public Result getByStudent(@PathVariable Integer studentId) { return Result.success(violationLogMapper.findByStudentId(studentId)); }

    @GetMapping("/recent")
    public Result recent(@RequestParam(defaultValue = "30") int days) {
        LocalDate ed = LocalDate.now();
        return Result.success(violationLogMapper.findRecent(ed.minusDays(days).toString(), ed.toString()));
    }

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

    @PutMapping
    public Result update(@RequestBody ViolationLog v) {
        if (v.getId() == null) return Result.error(400, "ID不能为空");
        violationLogMapper.update(v);
        aiAnswerCache.clear();
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        violationLogMapper.deleteById(id);
        aiAnswerCache.clear();
        return Result.success();
    }

    @DeleteMapping("/batch/{ids}")
    public Result deleteBatch(@PathVariable String ids) {
        for (String id : ids.split(",")) violationLogMapper.deleteById(Integer.parseInt(id));
        aiAnswerCache.clear();
        return Result.success();
    }
}
