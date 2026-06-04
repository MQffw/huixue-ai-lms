package com.itheima.controller;

import com.itheima.pojo.ClazzCountOption;
import com.itheima.pojo.DegreeOption;
import com.itheima.pojo.JobOption;
import com.itheima.pojo.PageResult;
import com.itheima.pojo.Result;
import com.itheima.service.EmpLogService;
import com.itheima.service.ReportService;
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
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private EmpLogService empLogService;

    //统计员工职位人数
    @GetMapping("/empJobData")
    public Result getEmpJobData(){
        log.info("统计员工职位人数");
        JobOption jobOption = reportService.getEmpJobData();
        return Result.success(jobOption);
    }

    //统计员工性别人数
    @GetMapping("empGenderData")
    public Result getEmpGenderData(){
        log.info("统计员工性别人数");
        List<Map<String,Object>> genderList = reportService.getEmpGenderData();
        return Result.success(genderList);
    }

    //统计学员学历
    @GetMapping("/studentDegreeData")
    public Result getStudentDegreeData(){
        log.info("统计学员学历");
        List<DegreeOption> degreeOptions = reportService.getStudentDegreeData();
        return Result.success(degreeOptions);
    }

    //统计班级人数
    @GetMapping("/studentCountData")
    public Result getStudentCountData(){
        log.info("统计班级人数");
        ClazzCountOption countOption = reportService.getStudentCountData();
        return Result.success(countOption);
    }

    //操作日志分页查询
    @GetMapping("/log/page")
    public Result getLogPage(Integer page, Integer pageSize){
        log.info("操作日志分页查询：page={}, pageSize={}", page, pageSize);
        PageResult pageResult = empLogService.page(page, pageSize);
        return Result.success(pageResult);
    }
}
