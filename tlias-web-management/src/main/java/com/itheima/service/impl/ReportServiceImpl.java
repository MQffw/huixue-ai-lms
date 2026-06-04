package com.itheima.service.impl;

import com.itheima.mapper.EmpMapper;
import com.itheima.mapper.StudentMapper;
import com.itheima.pojo.ClazzCountOption;
import com.itheima.pojo.DegreeOption;
import com.itheima.pojo.JobOption;
import com.itheima.service.ReportService;
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

        // 对班级名称进行智能截断处理，确保前端显示完整
        List<Object> clazzList = list.stream().map(dataMap -> {
            String clazzName = dataMap.get("pos").toString();
            return (Object) truncateClazzName(clazzName);
        }).toList();

        List<Object> dataList = list.stream().map(dataMap -> dataMap.get("num")).toList();
        return new ClazzCountOption(clazzList, dataList);
    }

    /**
     * 固定6个字符换行显示
     */
    private String truncateClazzName(String clazzName) {
        // 如果长度不超过6个字符，直接返回
        if (clazzName.length() <= 6) {
            return clazzName;
        }

        // 固定在第6个字符后换行
        return clazzName.substring(0, 6) + "\n" + clazzName.substring(6);
    }

}
