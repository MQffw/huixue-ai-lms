package com.huixuetong.controller;

import com.huixuetong.pojo.ClazzCountOption;
import com.huixuetong.pojo.DegreeOption;
import com.huixuetong.pojo.JobOption;
import com.huixuetong.pojo.PageResult;
import com.huixuetong.pojo.Result;
import com.huixuetong.service.EmpLogService;
import com.huixuetong.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping("/report")
@RestController
@Tag(name = "报表统计", description = "员工、学员数据统计与操作日志接口")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private EmpLogService empLogService;

    //统计员工职位人数
    @Operation(summary = "统计员工职位人数")
    @GetMapping("/empJobData")
    public Result getEmpJobData(){
        log.info("统计员工职位人数");
        JobOption jobOption = reportService.getEmpJobData();
        return Result.success(jobOption);
    }

    //统计员工性别人数
    @Operation(summary = "统计员工性别人数")
    @GetMapping("empGenderData")
    public Result getEmpGenderData(){
        log.info("统计员工性别人数");
        List<Map<String,Object>> genderList = reportService.getEmpGenderData();
        return Result.success(genderList);
    }

    //统计学员学历
    @Operation(summary = "统计学员学历分布")
    @GetMapping("/studentDegreeData")
    public Result getStudentDegreeData(){
        log.info("统计学员学历");
        List<DegreeOption> degreeOptions = reportService.getStudentDegreeData();
        return Result.success(degreeOptions);
    }

    //统计班级人数
    @Operation(summary = "统计班级人数")
    @GetMapping("/studentCountData")
    public Result getStudentCountData(){
        log.info("统计班级人数");
        ClazzCountOption countOption = reportService.getStudentCountData();
        return Result.success(countOption);
    }

    //操作日志分页查询
    @Operation(summary = "操作日志分页查询")
    @GetMapping("/log/page")
    public Result getLogPage(Integer page, Integer pageSize){
        log.info("操作日志分页查询：page={}, pageSize={}", page, pageSize);
        PageResult pageResult = empLogService.page(page, pageSize);
        return Result.success(pageResult);
    }
}