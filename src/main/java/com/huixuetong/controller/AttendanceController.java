package com.huixuetong.controller;

import com.huixuetong.mapper.AttendanceMapper;
import com.huixuetong.mapper.ClazzMapper;
import com.huixuetong.mapper.StudentMapper;
import com.huixuetong.ai.cache.AiAnswerCache;
import com.huixuetong.pojo.*;
import com.huixuetong.utils.CurrentHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping("/attendance")
@RestController
@Tag(name = "考勤管理", description = "考勤记录查询与统计接口")
public class AttendanceController {

    @Autowired private AttendanceMapper attendanceMapper;
    @Autowired private ClazzMapper clazzMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private AiAnswerCache aiAnswerCache;

    /** 分页+搜索（SQL 层过滤 + 分页，避免全量加载内存） */
    @Operation(summary = "考勤分页查询")
    @GetMapping
    public Result page(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) Integer clazzId,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate,
                       @RequestParam(required = false) String studentName,
                       @RequestParam(required = false) Integer status) {

        LocalDate start = (startDate != null && !startDate.isEmpty()) ? LocalDate.parse(startDate) : null;
        LocalDate end = (endDate != null && !endDate.isEmpty()) ? LocalDate.parse(endDate) : null;
        int offset = (page - 1) * pageSize;

        List<Attendance> records = attendanceMapper.findPage(clazzId, status, studentName, start, end, offset, pageSize);
        long total = attendanceMapper.countByCondition(clazzId, status, studentName, start, end);

        return Result.success(new PageResult<>(total, records));
    }

    @Operation(summary = "查询学员考勤记录")
    @GetMapping("/student/{studentId}")
    public Result getByStudent(@PathVariable Integer studentId) {
        return Result.success(attendanceMapper.findByStudentId(studentId));
    }

    @Operation(summary = "查询班级考勤记录")
    @GetMapping("/clazz/{clazzId}")
    public Result getByClazz(@PathVariable Integer clazzId, @RequestParam(required = false) String date) {
        LocalDate d = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
        return Result.success(attendanceMapper.findByClazzIdAndDate(clazzId, d));
    }

    @Operation(summary = "查询班级出勤率")
    @GetMapping("/rate/{clazzId}")
    public Result getRate(@PathVariable Integer clazzId) {
        LocalDate today = LocalDate.now();
        LocalDate ms = today.withDayOfMonth(1);
        List<Map<String, Object>> stats = attendanceMapper.countByStatusBetween(clazzId, ms, today);
        long total = 0, normal = 0, late = 0, early = 0, leave = 0, absent = 0;
        for (Map<String, Object> row : stats) {
            int st = ((Number) row.get("status")).intValue();
            long c = ((Number) row.get("count")).longValue();
            total += c;
            switch (st) { case 1 -> normal = c; case 2 -> late = c; case 3 -> early = c; case 4 -> leave = c; case 5 -> absent = c; }
        }
        return Result.success(Map.of("totalCount", total, "normalCount", normal, "lateCount", late,
                "earlyCount", early, "leaveCount", leave, "absentCount", absent,
                "attendanceRate", total > 0 ? String.format("%.1f", normal * 100.0 / total) : "0.0"));
    }

    // ======================== 写接口 ========================

    @Operation(summary = "新增考勤记录")
    @PostMapping
    public Result add(@RequestBody Attendance attendance) {
        validate(attendance);
        attendance.setRecordEmpId(CurrentHolder.getId());
        attendanceMapper.insert(attendance);
        aiAnswerCache.clear();
        log.info("新增考勤: studentId={}, clazzId={}, date={}, status={}",
                attendance.getStudentId(), attendance.getClazzId(),
                attendance.getAttendDate(), attendance.getStatus());
        return Result.success();
    }

    @Operation(summary = "修改考勤记录")
    @PutMapping
    public Result update(@RequestBody Attendance attendance) {
        if (attendance.getId() == null) {
            throw new IllegalArgumentException("考勤ID不能为空");
        }
        validate(attendance);
        attendanceMapper.update(attendance);
        aiAnswerCache.clear();
        log.info("修改考勤: id={}, status={}", attendance.getId(), attendance.getStatus());
        return Result.success();
    }

    @Operation(summary = "删除考勤记录")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        attendanceMapper.deleteById(id);
        aiAnswerCache.clear();
        log.info("删除考勤: id={}", id);
        return Result.success();
    }

    @Operation(summary = "批量删除考勤记录")
    @DeleteMapping("/batch/{ids}")
    public Result deleteBatch(@PathVariable List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("请选择要删除的记录");
        }
        int removed = attendanceMapper.deleteByIds(ids);
        aiAnswerCache.clear();
        log.info("批量删除考勤: ids={}, removed={}", ids, removed);
        return Result.success();
    }

    private void validate(Attendance a) {
        if (a.getStudentId() == null || studentMapper.getById(a.getStudentId()) == null) {
            throw new IllegalArgumentException("学员不存在");
        }
        if (a.getClazzId() == null || clazzMapper.getById(a.getClazzId()) == null) {
            throw new IllegalArgumentException("班级不存在");
        }
        if (a.getAttendDate() == null) {
            a.setAttendDate(LocalDate.now());
        }
        if (a.getStatus() == null || a.getStatus() < 1 || a.getStatus() > 5) {
            throw new IllegalArgumentException("考勤状态不合法（1正常 2迟到 3早退 4请假 5旷课）");
        }
    }

    /** 获取班级列表（前端下拉用） */
    @Operation(summary = "查询班级列表")
    @GetMapping("/clazzList")
    public Result clazzList() { return Result.success(clazzMapper.listAll()); }
}