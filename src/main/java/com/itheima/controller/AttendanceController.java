package com.itheima.controller;

import com.itheima.mapper.AttendanceMapper;
import com.itheima.mapper.ClazzMapper;
import com.itheima.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping("/attendance")
@RestController
public class AttendanceController {

    @Autowired private AttendanceMapper attendanceMapper;
    @Autowired private ClazzMapper clazzMapper;

    /** 分页+搜索（SQL 层过滤 + 分页，避免全量加载内存） */
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

    @GetMapping("/student/{studentId}")
    public Result getByStudent(@PathVariable Integer studentId) {
        return Result.success(attendanceMapper.findByStudentId(studentId));
    }

    @GetMapping("/clazz/{clazzId}")
    public Result getByClazz(@PathVariable Integer clazzId, @RequestParam(required = false) String date) {
        LocalDate d = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
        return Result.success(attendanceMapper.findByClazzIdAndDate(clazzId, d));
    }

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

    /** 获取班级列表（前端下拉用） */
    @GetMapping("/clazzList")
    public Result clazzList() { return Result.success(clazzMapper.listAll()); }
}
