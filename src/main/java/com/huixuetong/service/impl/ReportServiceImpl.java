package com.huixuetong.service.impl;

import com.huixuetong.mapper.EmpMapper;
import com.huixuetong.mapper.StudentMapper;
import com.huixuetong.pojo.ClazzCountOption;
import com.huixuetong.pojo.DegreeOption;
import com.huixuetong.pojo.JobOption;
import com.huixuetong.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private EmpMapper empMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Override
    public JobOption getEmpJobData(){
        //调用mapper接口
        List<Map<String, Object>> list = empMapper.countEmpJobData();
        //组装结果返回
        List<Object> jobList = list.stream().map(dataMap -> dataMap.get("pos")).toList();
        List<Object> dataList = list.stream().map(dataMap -> dataMap.get("num")).toList();
        return new JobOption(jobList,dataList);
    }

    @Override
    public List<Map<String, Object>> getEmpGenderData() {
        return empMapper.countEmpGenderData();
    }

    @Override
    public List<DegreeOption> getStudentDegreeData() {
        List<Map<String, Object>> list = studentMapper.countStudentDegreeData();
        return list.stream()
                .map(dataMap -> new DegreeOption(
                    dataMap.get("pos").toString(),
                    Integer.valueOf(dataMap.get("num").toString())
                ))
                .toList();
    }

    @Override
    public ClazzCountOption getStudentCountData() {
        List<Map<String, Object>> list = studentMapper.countStudentClazzData();

        List<Object> clazzList = list.stream().map(dataMap -> dataMap.get("pos")).toList();

        List<Object> dataList = list.stream().map(dataMap -> dataMap.get("num")).toList();
        return new ClazzCountOption(clazzList, dataList);
    }


}
