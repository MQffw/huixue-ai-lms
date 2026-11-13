package com.huixuetong.controller;

import com.huixuetong.mapper.CourseScheduleMapper;
import com.huixuetong.pojo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 课表查询（只读）
 * 排课规则由教务导入，本系统只提供查询，避免重复实现冲突检测/场地/教师时间等复杂规则。
 */
@Slf4j
@RestController
@RequestMapping("/schedules")
@Tag(name = "课表查询", description = "按班级/日期查询排课")
public class CourseScheduleController {

    @Autowired
    private CourseScheduleMapper courseScheduleMapper;

    @Operation(summary = "按日期或班级查课表")
    @GetMapping
    public Result list(@RequestParam(required = false) Integer clazzId,
                       @RequestParam(required = false) String date) {
        LocalDate day = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
        if (clazzId != null) {
            return Result.success(courseScheduleMapper.findByClazzIdAndDate(clazzId, day));
        }
        return Result.success(courseScheduleMapper.findByDate(day));
    }

    @Operation(summary = "查询某班级全部课表")
    @GetMapping("/clazz/{clazzId}")
    public Result byClazz(@PathVariable Integer clazzId) {
        return Result.success(courseScheduleMapper.findByClazzId(clazzId));
    }
}
