package com.itheima.ai.tool;

import com.itheima.mapper.ClazzMapper;
import com.itheima.mapper.CourseMapper;
import com.itheima.mapper.CourseScheduleMapper;
import com.itheima.pojo.Clazz;
import com.itheima.pojo.Course;
import com.itheima.pojo.CourseSchedule;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 课程 / 排课域 Tool（4 个）
 */
@Component
public class CourseTools {

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private CourseScheduleMapper scheduleMapper;

    @Autowired
    private ClazzMapper clazzMapper;

    /**
     * 查所有课程列表
     */
    @Tool(description = """
            查询所有课程列表（不分学科）。无参数。
            返回：每行 "课程名（学科名，N课时）"
            """)
    public String getCourseList() {
        List<Course> list = courseMapper.findAll();
        if (list.isEmpty()) return "暂无课程数据";
        return "共" + list.size() + "门课程：\n" +
                list.stream().map(c -> c.getName() + "（" + formatSubject(c.getSubject()) + "，" + c.getHours() + "课时）")
                        .collect(Collectors.joining("\n"));
    }

    /**
     * 按学科查课程
     */
    @Tool(description = """
            按学科方向查询课程列表。
            参数：subject（整数，1=Java 2=前端 3=大数据 4=Python 5=Go 6=嵌入式）
            返回："XX方向课程（共N门）：\n课程名（课时）\n..."
            """)
    public String getCourseBySubject(Integer subject) {
        List<Course> list = courseMapper.findBySubject(subject);
        if (list.isEmpty()) return "该学科暂无课程";
        return formatSubject(subject) + "方向课程（共" + list.size() + "门）：\n" +
                list.stream().map(c -> c.getName() + "（" + c.getHours() + "课时）")
                        .collect(Collectors.joining("\n"));
    }

    /**
     * 查今天的排课
     */
    @Tool(description = """
            查询今天的全部排课安排。无参数。
            返回：按班级分组 "HH:MM-HH:MM 课程名 教师 教室"；无排课返回 "今天没有排课安排"
            """)
    public String getTodaySchedule() {
        LocalDate today = LocalDate.now();
        List<CourseSchedule> list = scheduleMapper.findByDate(today);
        if (list.isEmpty()) return "今天没有排课安排";
        StringBuilder sb = new StringBuilder("今日排课（" + today + "）：\n");
        String lastClazz = "";
        for (CourseSchedule cs : list) {
            if (!cs.getClazzName().equals(lastClazz)) {
                sb.append("\n【").append(cs.getClazzName()).append("】\n");
                lastClazz = cs.getClazzName();
            }
            sb.append(cs.getStartTime()).append("-").append(cs.getEndTime())
              .append(" ").append(cs.getCourseName())
              .append(" ").append(cs.getTeacherName())
              .append(" ").append(cs.getRoom()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 按班级查排课
     */
    @Tool(description = """
            查询指定班级的全部排课安排。
            参数：clazzId（整数，必填）
            返回：按日期分组，每行 "  HH:MM-HH:MM 课程 教师 教室"；无排课返回 "该班级暂无排课数据"
            """)
    public String getClassSchedule(Integer clazzId) {
        List<CourseSchedule> list = scheduleMapper.findByClazzId(clazzId);
        if (list.isEmpty()) return "该班级暂无排课数据";
        Clazz clazz = clazzMapper.getById(clazzId);
        String clazzName = clazz != null ? clazz.getName() : "ID=" + clazzId;
        StringBuilder sb = new StringBuilder(clazzName + "排课安排：\n");
        String lastDate = "";
        for (CourseSchedule cs : list) {
            String dateStr = cs.getClassDate().toString();
            if (!dateStr.equals(lastDate)) {
                sb.append("\n").append(dateStr).append("：\n");
                lastDate = dateStr;
            }
            sb.append("  ").append(cs.getStartTime()).append("-").append(cs.getEndTime())
              .append(" ").append(cs.getCourseName())
              .append(" ").append(cs.getTeacherName())
              .append(" ").append(cs.getRoom()).append("\n");
        }
        return sb.toString();
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
