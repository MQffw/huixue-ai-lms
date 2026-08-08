package com.itheima.ai.tool;

import com.itheima.mapper.ClazzMapper;
import com.itheima.mapper.DeptMapper;
import com.itheima.pojo.Clazz;
import com.itheima.pojo.Dept;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 班级 / 部门域 Tool（4 个）
 */
@Component
public class ClazzTools {

    @Autowired
    private ClazzMapper clazzMapper;

    @Autowired
    private DeptMapper deptMapper;

    /**
     * 查所有班级列表
     */
    @Tool(description = """
            查询所有班级列表。无参数。
            返回：每行一个班级，ID=.., 班级名称=.., 教室=.., 班主任=.., 学科=.., 开课日期=.., 结课日期=..
            """)
    public String listAllClazz() {
        List<Clazz> list = clazzMapper.listAll();
        if (list.isEmpty()) return "暂无班级数据";
        return "共" + list.size() + "个班级：\n" +
                list.stream().map(this::formatClazz).reduce((a, b) -> a + "\n---\n" + b).orElse("");
    }

    /**
     * 按班级 ID 查询班级详情
     */
    @Tool(description = """
            按班级ID查询单个班级详情。
            参数：clazzId（整数，必填）
            返回：ID=.., 班级名称=.., 教室=.., 班主任=.., 学科=.., 开课/结课日期
            """)
    public String getClazzById(Integer clazzId) {
        Clazz c = clazzMapper.getById(clazzId);
        if (c == null) return "未找到ID为" + clazzId + "的班级";
        return formatClazz(c);
    }

    /**
     * 查所有部门
     */
    @Tool(description = """
            查询所有部门列表。无参数。
            返回：每行 "ID=X, 名称=XX"
            """)
    public String listAllDepts() {
        List<Dept> list = deptMapper.findAll();
        if (list.isEmpty()) return "暂无部门数据";
        StringBuilder sb = new StringBuilder("共" + list.size() + "个部门：\n");
        for (Dept d : list) {
            sb.append("ID=").append(d.getId()).append(", 名称=").append(d.getName()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 按部门 ID 查询
     */
    @Tool(description = """
            按部门ID查询单个部门。
            参数：deptId（整数，必填）
            返回："部门ID=X, 名称=XX"
            """)
    public String getDeptById(Integer deptId) {
        Dept d = deptMapper.getById(deptId);
        if (d == null) return "未找到ID为" + deptId + "的部门";
        return "部门ID=" + d.getId() + ", 名称=" + d.getName();
    }

    private String formatClazz(Clazz c) {
        return "ID=" + c.getId() +
                ", 班级名称=" + c.getName() +
                ", 教室=" + c.getRoom() +
                (c.getMasterName() != null ? ", 班主任=" + c.getMasterName() : "") +
                ", 学科=" + formatSubject(c.getSubject()) +
                ", 开课日期=" + c.getBeginDate() +
                ", 结课日期=" + c.getEndDate();
    }

    private String formatSubject(Integer subject) {
        if (subject == null) return "未知";
        return switch (subject) {
            case 1 -> "Java";
            case 2 -> "前端";
            case 3 -> "大数据";
            case 4 -> "Python";
            case 5 -> "Go";
            case 6 -> "嵌入式";
            default -> "未知";
        };
    }
}
