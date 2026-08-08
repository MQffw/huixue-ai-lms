package com.itheima.ai.tool;

import com.itheima.mapper.EmpExprMapper;
import com.itheima.mapper.EmpMapper;
import com.itheima.pojo.Emp;
import com.itheima.pojo.EmpExpr;
import com.itheima.pojo.EmpQueryParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 员工域 Tool（7 个）
 */
@Component
public class EmployeeTools {

    @Autowired
    private EmpMapper empMapper;

    @Autowired
    private EmpExprMapper empExprMapper;

    /**
     * 按员工 ID 查询详情
     */
    @Tool(description = """
            按员工ID查询单个员工详情。
            参数：empId（正整数，与 student.id 不同源）
            返回：ID=.., 姓名=.., 性别=男/女, 职位=讲师/..., 薪资=N元, 部门=.., 入职日期=yyyy-MM-dd
            边界：未找到返回 "未找到ID为X的员工"
            """)
    public String getEmpById(Integer empId) {
        Emp e = empMapper.getById(empId);
        if (e == null) return "未找到ID为" + empId + "的员工";
        return formatEmp(e);
    }

    /**
     * 按姓名模糊搜索员工
     */
    @Tool(description = """
            按姓名模糊搜索员工（LIKE %name%）。
            参数：name（至少1字符）
            返回：最多100条，每条一行 KV 用 --- 分隔；无结果返回 "未找到姓名包含「X」的员工"
            """)
    public String findEmpsByName(String name) {
        EmpQueryParam param = new EmpQueryParam();
        param.setName(name);
        param.setPageSize(100);
        List<Emp> list = empMapper.pageList(param);
        if (list.isEmpty()) return "未找到姓名包含「" + name + "」的员工";
        return list.stream().map(this::formatEmp).collect(Collectors.joining("\n---\n"));
    }

    /**
     * 查所有员工列表
     */
    @Tool(description = """
            查询所有员工列表（无参数，建议仅在需要全量时使用）。
            返回："共N名员工：" + 每条一行 KV
            """)
    public String listAllEmps() {
        List<Emp> list = empMapper.findAll();
        if (list.isEmpty()) return "暂无员工数据";
        return "共" + list.size() + "名员工：\n" +
                list.stream().map(this::formatEmp).collect(Collectors.joining("\n---\n"));
    }

    /**
     * 按职位统计员工人数
     */
    @Tool(description = """
            按职位统计员工人数分布（GROUP BY job）。
            无参数。
            返回：每行 "职位名: N人"（班主任/讲师/学工主管/教研主管/咨询师）
            """)
    public String countEmpsByJob() {
        List<Map<String, Object>> data = empMapper.countEmpJobData();
        if (data.isEmpty()) return "暂无员工职位统计数据";
        StringBuilder sb = new StringBuilder("各职位员工人数：\n");
        for (Map<String, Object> row : data) {
            sb.append(row.get("pos")).append(": ").append(row.get("num")).append("人\n");
        }
        return sb.toString();
    }

    /**
     * 按性别统计员工人数
     */
    @Tool(description = """
            按性别统计员工人数分布（GROUP BY gender）。
            无参数。
            返回：每行 "男: N人" / "女: N人"
            """)
    public String countEmpsByGender() {
        List<Map<String, Object>> data = empMapper.countEmpGenderData();
        if (data.isEmpty()) return "暂无员工性别统计数据";
        StringBuilder sb = new StringBuilder("员工性别分布：\n");
        for (Map<String, Object> row : data) {
            sb.append(row.get("pos")).append(": ").append(row.get("num")).append("人\n");
        }
        return sb.toString();
    }

    /**
     * 查某个员工的工作经历
     */
    @Tool(description = """
            查询指定员工的工作经历列表（emp_expr 表）。
            参数：empId（整数）
            返回：每行 "yyyy-MM至yyyy-MM，在XX公司担任XX"；无记录返回 "该员工没有工作经历记录"
            """)
    public String getEmpWorkExperience(Integer empId) {
        List<EmpExpr> list = empExprMapper.findByEmpId(empId);
        if (list.isEmpty()) return "该员工没有工作经历记录";
        Emp emp = empMapper.getById(empId);
        String empName = emp != null ? emp.getName() : "ID=" + empId;
        StringBuilder sb = new StringBuilder(empName + "的工作经历：\n");
        for (EmpExpr expr : list) {
            sb.append(expr.getBegin()).append(" 至 ")
              .append(expr.getEnd() != null ? expr.getEnd() : "至今")
              .append("，在").append(expr.getCompany())
              .append("担任").append(expr.getJob()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 查有工作经历记录的所有员工
     */
    @Tool(description = """
            查询所有员工的工作经历汇总（无参数）。
            返回：按员工姓名分组 "姓名：N段经历"；无记录返回 "系统中没有员工工作经历记录"
            """)
    public String listEmpsWithWorkExperience() {
        List<Map<String, Object>> allExpr = empExprMapper.findAllWithEmpName();
        if (allExpr.isEmpty()) return "系统中没有员工工作经历记录";
        Map<String, Integer> countMap = new HashMap<>();
        for (Map<String, Object> row : allExpr) {
            String name = (String) row.get("empName");
            countMap.merge(name, 1, Integer::sum);
        }
        StringBuilder sb = new StringBuilder("共有" + countMap.size() + "名员工有工作经历记录：\n");
        countMap.forEach((name, count) ->
            sb.append(name).append("：").append(count).append("段经历\n"));
        return sb.toString();
    }

    private String formatEmp(Emp e) {
        return "ID=" + e.getId() +
                ", 姓名=" + e.getName() +
                ", 性别=" + (e.getGender() != null && e.getGender() == 1 ? "男" : "女") +
                ", 职位=" + formatJob(e.getJob()) +
                ", 薪资=" + (e.getSalary() != null ? e.getSalary() + "元" : "未知") +
                (e.getDeptName() != null ? ", 部门=" + e.getDeptName() : "") +
                ", 入职日期=" + e.getEntryDate();
    }

    private String formatJob(Integer job) {
        if (job == null) return "未知";
        return switch (job) {
            case 1 -> "班主任";
            case 2 -> "讲师";
            case 3 -> "学工主管";
            case 4 -> "教研主管";
            case 5 -> "咨询师";
            default -> "未知";
        };
    }
}
