package com.itheima.ai.tool;

import com.itheima.mapper.ClazzMapper;
import com.itheima.mapper.StudentMapper;
import com.itheima.pojo.Clazz;
import com.itheima.pojo.Student;
import com.itheima.pojo.StudentQueryParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学员域 Tool（6 个）
 *
 * 模板化 description 格式：
 *   用途 | 参数 | 返回格式 | 边界/限制
 */
@Component
public class StudentTools {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private ClazzMapper clazzMapper;

    /**
     * 按学员 ID 查询详情
     * @param studentId 学员 ID（正整数，与 emp.id 不同源）
     * @return 姓名/学号/性别/学历/班级/违纪次数等 KV；未找到返回 "未找到ID为X的学员"
     */
    @Tool(description = """
            按学员ID查询单个学员详情。
            参数：
              - studentId: 学员ID（正整数，必填）
            返回格式：ID=.., 姓名=.., 学号=.., 性别=男/女, 学历=本科/..., 班级=.., 违纪次数=N, 违纪扣分=N
            边界：未找到时返回 "未找到ID为X的学员"
            示例：getStudentById(1) → "ID=1, 姓名=张三, 学号=2024001, 性别=男, 学历=本科, 班级=Java1班"
            """)
    public String getStudentById(Integer studentId) {
        Student s = studentMapper.getById(studentId);
        if (s == null) return "未找到ID为" + studentId + "的学员";
        return formatStudent(s);
    }

    /**
     * 按姓名模糊搜索学员
     * @param name 支持部分匹配，至少 1 个字符
     * @return 最多 100 条记录；每条用 "\n---\n" 分隔；无结果返回 "未找到姓名包含「X」的学员"
     */
    @Tool(description = """
            按姓名模糊搜索学员（LIKE %name%）。
            参数：
              - name: 姓名关键词（至少1个字符，必填）
            返回格式：每条记录一行（ID=..,姓名=..,...），最多100条，用 --- 分隔
            边界：无结果时返回 "未找到姓名包含「X」的学员"
            示例：findStudentsByName("张") → "ID=1, 姓名=张三, ...\n---\nID=2, 姓名=张四, ..."
            """)
    public String findStudentsByName(String name) {
        StudentQueryParam param = new StudentQueryParam();
        param.setName(name);
        param.setPageSize(100);
        List<Student> list = studentMapper.pageList(param);
        if (list.isEmpty()) return "未找到姓名包含「" + name + "」的学员";
        return list.stream().map(this::formatStudent).collect(Collectors.joining("\n---\n"));
    }

    /**
     * 按班级 ID 查学员列表
     * @param clazzId 班级 ID
     * @return "共N名学员：" + 学员 KV 列表；无结果返回 "班级ID=X下没有学员"
     */
    @Tool(description = """
            查询某班级下的全部学员列表。
            参数：
              - clazzId: 班级ID（整数，必填）
            返回格式："共N名学员：\n" + 每行一条学员记录
            边界：无学员时返回 "班级ID=X下没有学员"
            """)
    public String findStudentsByClazzId(Integer clazzId) {
        StudentQueryParam param = new StudentQueryParam();
        param.setClazzId(clazzId);
        param.setPageSize(100);
        List<Student> list = studentMapper.pageList(param);
        if (list.isEmpty()) return "班级ID=" + clazzId + "下没有学员";
        return "共" + list.size() + "名学员：\n" +
                list.stream().map(this::formatStudent).collect(Collectors.joining("\n---\n"));
    }

    /**
     * 按学历统计学员人数（无需参数）
     * @return 每行 "学历名: N人"；无数据返回 "暂无学生学历统计数据"
     */
    @Tool(description = """
            按学历统计学员人数分布（GROUP BY degree）。
            无参数。
            返回格式：每行 "学历名: N人"，本科/硕士/博士/大专/高中/初中
            边界：无学生时返回 "暂无学生学历统计数据"
            示例："各学历学生人数：\n本科: 356人\n硕士: 12人"
            """)
    public String countStudentsByDegree() {
        List<Map<String, Object>> data = studentMapper.countStudentDegreeData();
        if (data.isEmpty()) return "暂无学生学历统计数据";
        StringBuilder sb = new StringBuilder("各学历学生人数：\n");
        for (Map<String, Object> row : data) {
            sb.append(row.get("pos")).append(": ").append(row.get("num")).append("人\n");
        }
        return sb.toString();
    }

    /**
     * 按性别统计学员人数（无需参数）
     * @return 每行 "男: N人" 或 "女: N人"；无数据返回 "暂无学生性别统计数据"
     */
    @Tool(description = """
            按性别统计学员人数分布（GROUP BY gender）。
            无参数。
            返回格式：每行 "男: N人" / "女: N人"
            边界：无数据时返回 "暂无学生性别统计数据"
            """)
    public String countStudentsByGender() {
        List<Map<String, Object>> data = studentMapper.countStudentGenderData();
        if (data.isEmpty()) return "暂无学生性别统计数据";
        StringBuilder sb = new StringBuilder("学生性别分布：\n");
        for (Map<String, Object> row : data) {
            sb.append(row.get("pos")).append(": ").append(row.get("num")).append("人\n");
        }
        return sb.toString();
    }

    /**
     * 按班级统计学员人数（无需参数）
     * @return 每行 "班级名: N人"；无数据返回 "暂无班级学生统计数据"
     */
    @Tool(description = """
            按班级统计学员人数分布（GROUP BY clazz_id）。
            无参数。
            返回格式：每行 "班级名称: N人"
            边界：无数据时返回 "暂无班级学生统计数据"
            """)
    public String countStudentsByClazz() {
        List<Map<String, Object>> data = studentMapper.countStudentClazzData();
        if (data.isEmpty()) return "暂无班级学生统计数据";
        StringBuilder sb = new StringBuilder("各班级学生人数：\n");
        for (Map<String, Object> row : data) {
            sb.append(row.get("pos")).append(": ").append(row.get("num")).append("人\n");
        }
        return sb.toString();
    }

    private String formatStudent(Student s) {
        return "ID=" + s.getId() +
                ", 姓名=" + s.getName() +
                ", 学号=" + s.getNo() +
                ", 性别=" + (s.getGender() != null && s.getGender() == 1 ? "男" : "女") +
                ", 学历=" + formatDegree(s.getDegree()) +
                (s.getClazzName() != null ? ", 班级=" + s.getClazzName() : "") +
                ", 违纪次数=" + s.getViolationCount() +
                ", 违纪扣分=" + s.getViolationScore();
    }

    private String formatDegree(Integer degree) {
        if (degree == null) return "未知";
        return switch (degree) {
            case 1 -> "初中";
            case 2 -> "高中";
            case 3 -> "大专";
            case 4 -> "本科";
            case 5 -> "硕士";
            case 6 -> "博士";
            default -> "未知";
        };
    }
}
