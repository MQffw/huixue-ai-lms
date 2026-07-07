package com.itheima.ai.tool;

import com.itheima.mapper.*;
import com.itheima.pojo.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 考勤 / 成绩 / 违纪 / 缴费 / 就业 域 Tool（14 个）
 */
@Component
public class AffairsTools {

    @Autowired
    private AttendanceMapper attendanceMapper;
    @Autowired
    private ScoreMapper scoreMapper;
    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private ViolationLogMapper violationLogMapper;
    @Autowired
    private PaymentMapper paymentMapper;
    @Autowired
    private EmploymentMapper employmentMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private ClazzMapper clazzMapper;

    // ========== 考勤（4） ==========

    /**
     * 按学员查考勤记录
     */
    @Tool(description = """
            查询指定学员的考勤记录。
            参数：studentId（整数，必填）
            返回：每行 "yyyy-MM-dd：状态(备注)"；无记录返回 "该学员暂无考勤记录"
            """)
    public String getStudentAttendance(Integer studentId) {
        List<Attendance> list = attendanceMapper.findByStudentId(studentId);
        if (list.isEmpty()) return "该学员暂无考勤记录";
        Student s = studentMapper.getById(studentId);
        String name = s != null ? s.getName() : "ID=" + studentId;
        StringBuilder sb = new StringBuilder(name + "的考勤记录（最近" + list.size() + "条）：\n");
        for (Attendance a : list) {
            sb.append(a.getAttendDate()).append("：").append(formatAttendStatus(a.getStatus()));
            if (a.getRemark() != null) sb.append("（").append(a.getRemark()).append("）");
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 按班级+日期查考勤
     */
    @Tool(description = """
            查询指定班级某天的考勤情况（全学员）。
            参数：
              - clazzId: 班级ID（整数，必填）
              - dateStr: 日期 yyyy-MM-dd（可缺省，默认今天）
            返回：每行 "学员名：状态(备注)"，末尾汇总 "正常N人，旷课N人，共N人"
            """)
    public String getClassAttendance(Integer clazzId, String dateStr) {
        LocalDate date = (dateStr != null && !dateStr.isEmpty()) ? LocalDate.parse(dateStr) : LocalDate.now();
        List<Attendance> list = attendanceMapper.findByClazzIdAndDate(clazzId, date);
        if (list.isEmpty()) return date + "该班级没有考勤记录";
        Clazz clazz = clazzMapper.getById(clazzId);
        String clazzName = clazz != null ? clazz.getName() : "ID=" + clazzId;
        StringBuilder sb = new StringBuilder(clazzName + " " + date + " 考勤情况：\n");
        int normalCount = 0, absentCount = 0;
        for (Attendance a : list) {
            sb.append(a.getStudentName()).append("：").append(formatAttendStatus(a.getStatus()));
            if (a.getRemark() != null) sb.append("（").append(a.getRemark()).append("）");
            sb.append("\n");
            if (a.getStatus() == 1) normalCount++;
            if (a.getStatus() == 5) absentCount++;
        }
        sb.append("\n正常").append(normalCount).append("人，旷课").append(absentCount).append("人，共").append(list.size()).append("人");
        return sb.toString();
    }

    /**
     * 按班查出勤率
     */
    @Tool(description = """
            查询某班级本月的出勤率统计。
            参数：clazzId（整数，必填）
            返回：从月初到今天的出勤率 %、各状态次数；无数据返回 "该班级本月暂无考勤数据"
            """)
    public String getAttendanceRate(Integer clazzId) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.withDayOfMonth(1);
        List<Map<String, Object>> stats = attendanceMapper.countByStatusBetween(clazzId, startDate, today);
        if (stats.isEmpty()) return "该班级本月暂无考勤数据";
        Map<Integer, Long> statusCount = new HashMap<>();
        long total = 0;
        for (Map<String, Object> row : stats) {
            Integer status = ((Number) row.get("status")).intValue();
            Long count = ((Number) row.get("count")).longValue();
            statusCount.put(status, count);
            total += count;
        }
        Clazz clazz = clazzMapper.getById(clazzId);
        String clazzName = clazz != null ? clazz.getName() : "ID=" + clazzId;
        long normal = statusCount.getOrDefault(1, 0L);
        double rate = total > 0 ? (normal * 100.0 / total) : 0;
        return clazzName + "本月出勤统计（" + startDate + "至" + today + "）：\n"
                + "总记录" + total + "条，正常" + normal + "条，出勤率" + String.format("%.1f%%", rate) + "\n"
                + "迟到" + statusCount.getOrDefault(2, 0L) + "次，早退" + statusCount.getOrDefault(3, 0L)
                + "次，请假" + statusCount.getOrDefault(4, 0L) + "次，旷课" + statusCount.getOrDefault(5, 0L) + "次";
    }

    /**
     * 查某天考勤异常学员
     */
    @Tool(description = """
            查询某天考勤异常（迟到/早退/请假/旷课）的学员列表。
            参数：dateStr yyyy-MM-dd（可缺省默认今天）
            返回：每行 "学员名（班级）：状态(备注)"；无异常返回 "X 所有学员考勤正常，无异常记录"
            """)
    public String getAbnormalAttendance(String dateStr) {
        LocalDate date = (dateStr != null && !dateStr.isEmpty()) ? LocalDate.parse(dateStr) : LocalDate.now();
        List<Attendance> list = attendanceMapper.findAbnormalByDate(date);
        if (list == null || list.isEmpty()) return date + " 所有学员考勤正常，无异常记录";
        StringBuilder sb = new StringBuilder(date + " 考勤异常学员共" + list.size() + "人：\n");
        for (Attendance a : list) {
            sb.append(a.getStudentName() != null ? a.getStudentName() : "学员" + a.getStudentId())
              .append("（").append(a.getClazzName() != null ? a.getClazzName() : "未知班级").append("）")
              .append("：").append(formatAttendStatus(a.getStatus()));
            if (a.getRemark() != null) sb.append("（").append(a.getRemark()).append("）");
            sb.append("\n");
        }
        return sb.toString();
    }

    // ========== 成绩（3） ==========

    /**
     * 按学员查成绩
     */
    @Tool(description = """
            查询指定学员的全部成绩记录。
            参数：studentId（整数，必填）
            返回：每行 "考试名：X分（第N名） 备注"；无记录返回 "该学员暂无成绩记录"
            """)
    public String getStudentScores(Integer studentId) {
        List<Score> list = scoreMapper.findByStudentId(studentId);
        if (list.isEmpty()) return "该学员暂无成绩记录";
        Student s = studentMapper.getById(studentId);
        String name = s != null ? s.getName() : "ID=" + studentId;
        StringBuilder sb = new StringBuilder(name + "的成绩记录：\n");
        for (Score sc : list) {
            sb.append(sc.getExamName()).append("：").append(sc.getScore()).append("分");
            if (sc.getRank() != null) sb.append("（第").append(sc.getRank()).append("名）");
            if (sc.getRemark() != null) sb.append(" ").append(sc.getRemark());
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 按考试查班级平均分
     */
    @Tool(description = """
            查询某次考试的全班成绩统计。
            参数：examId（整数，必填）
            返回：参考人数、平均分、最高分、最低分、不及格人数；无成绩返回 "X暂无成绩数据"
            """)
    public String getClassAvgScore(Integer examId) {
        Exam exam = examMapper.getById(examId);
        String examName = exam != null ? exam.getName() : "ID=" + examId;
        Map<String, Object> stats = scoreMapper.getExamStats(examId);
        if (stats == null || stats.get("total_count") == null) return examName + "暂无成绩数据";
        long totalCount = ((Number) stats.get("total_count")).longValue();
        if (totalCount == 0) return examName + "暂无成绩数据";
        return examName + "成绩统计：\n"
                + "参考人数" + totalCount + "人，平均分" + String.format("%.1f", ((Number) stats.get("avg_score")).doubleValue())
                + "，最高分" + stats.get("max_score") + "，最低分" + stats.get("min_score")
                + "，不及格" + stats.get("fail_count") + "人";
    }

    /**
     * 按考试查排名
     */
    @Tool(description = """
            查询某次考试的班级排名（按分数降序）。
            参数：examId（整数，必填）
            返回：每行 "N. 姓名：X分（备注）"；无成绩返回 "该考试暂无成绩数据"
            """)
    public String getExamRanking(Integer examId) {
        List<Score> list = scoreMapper.findByExamId(examId);
        if (list.isEmpty()) return "该考试暂无成绩数据";
        Exam exam = examMapper.getById(examId);
        String examName = exam != null ? exam.getName() : "ID=" + examId;
        StringBuilder sb = new StringBuilder(examName + "成绩排名：\n");
        int rank = 1;
        for (Score sc : list) {
            sb.append(rank++).append(". ").append(sc.getStudentName())
              .append("：").append(sc.getScore()).append("分");
            if (sc.getRemark() != null) sb.append("（").append(sc.getRemark()).append("）");
            sb.append("\n");
        }
        return sb.toString();
    }

    // ========== 违纪（2） ==========

    /**
     * 按学员查违纪
     */
    @Tool(description = """
            查询指定学员的违纪记录。
            参数：studentId（整数，必填）
            返回：每行 "yyyy-MM-dd：违纪类型，扣N分（描述）" + 累计扣分；无记录返回 "该学员暂无违纪记录"
            """)
    public String getStudentViolations(Integer studentId) {
        List<ViolationLog> list = violationLogMapper.findByStudentId(studentId);
        if (list.isEmpty()) return "该学员暂无违纪记录";
        Student s = studentMapper.getById(studentId);
        String name = s != null ? s.getName() : "ID=" + studentId;
        StringBuilder sb = new StringBuilder(name + "的违纪记录（" + list.size() + "条）：\n");
        int totalDeduct = 0;
        for (ViolationLog vl : list) {
            sb.append(vl.getViolationDate()).append("：").append(vl.getViolationType())
              .append("，扣").append(vl.getDeductScore()).append("分");
            if (vl.getDescription() != null) sb.append("（").append(vl.getDescription()).append("）");
            sb.append("\n");
            totalDeduct += vl.getDeductScore();
        }
        sb.append("累计扣分：").append(totalDeduct).append("分");
        return sb.toString();
    }

    /**
     * 查最近 N 天违纪
     */
    @Tool(description = """
            查询最近一段时间（默认30天）的违纪记录。
            参数：days（整数，可选，默认30）
            返回：每行 "日期 姓名：违纪类型，扣N分"；无记录返回 "最近N天暂无违纪记录"
            """)
    public String getRecentViolations(Integer days) {
        if (days == null || days <= 0) days = 30;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        List<ViolationLog> list = violationLogMapper.findRecent(startDate.toString(), endDate.toString());
        if (list.isEmpty()) return "最近" + days + "天暂无违纪记录";
        StringBuilder sb = new StringBuilder("最近" + days + "天违纪记录（" + list.size() + "条）：\n");
        for (ViolationLog vl : list) {
            sb.append(vl.getViolationDate()).append(" ").append(vl.getStudentName())
              .append("：").append(vl.getViolationType()).append("，扣").append(vl.getDeductScore()).append("分\n");
        }
        return sb.toString();
    }

    // ========== 缴费（3） ==========

    /**
     * 按学员查缴费状态
     */
    @Tool(description = """
            查询指定学员的缴费记录。
            参数：studentId（整数，必填）
            返回：每行 "日期 支付方式 X元（状态标记）" + 合计已确认金额；无记录返回 "该学员暂无缴费记录"
            """)
    public String getStudentPaymentStatus(Integer studentId) {
        List<Payment> list = paymentMapper.findByStudentId(studentId);
        if (list.isEmpty()) return "该学员暂无缴费记录";
        Student s = studentMapper.getById(studentId);
        String name = s != null ? s.getName() : "ID=" + studentId;
        StringBuilder sb = new StringBuilder(name + "的缴费记录：\n");
        BigDecimal totalPaid = BigDecimal.ZERO;
        for (Payment p : list) {
            sb.append(p.getPaymentDate()).append(" ").append(p.getPaymentType())
              .append(" ").append(p.getAmount()).append("元（").append(p.getPaymentMethod()).append("）");
            if (p.getStatus() == 2) sb.append("【待确认】");
            else if (p.getStatus() == 3) sb.append("【已退款】");
            sb.append("\n");
            if (p.getStatus() == 1) totalPaid = totalPaid.add(p.getAmount());
        }
        sb.append("已确认缴费合计：").append(totalPaid).append("元");
        return sb.toString();
    }

    /**
     * 按班查缴费完成率
     */
    @Tool(description = """
            查询指定班级的缴费完成率。
            参数：clazzId（整数，必填）
            返回："总N人，已缴清N人，未缴清N人，缴费完成率X.X%"
            """)
    public String getClassPaymentRate(Integer clazzId) {
        List<Student> students = studentMapper.pageList(new StudentQueryParam() {{ setClazzId(clazzId); setPageSize(100); }});
        if (students.isEmpty()) return "该班级暂无学员";
        int paidCount = 0, unpaidCount = 0;
        for (Student st : students) {
            List<Payment> payments = paymentMapper.findByStudentId(st.getId());
            boolean hasUnpaid = payments.stream().anyMatch(p -> p.getStatus() == 2);
            if (hasUnpaid || payments.isEmpty()) unpaidCount++;
            else paidCount++;
        }
        Clazz clazz = clazzMapper.getById(clazzId);
        String clazzName = clazz != null ? clazz.getName() : "ID=" + clazzId;
        double rate = students.size() > 0 ? (paidCount * 100.0 / students.size()) : 0;
        return clazzName + "缴费统计：总" + students.size() + "人，已缴清" + paidCount + "人，未缴清" + unpaidCount + "人，缴费完成率" + String.format("%.1f%%", rate);
    }

    /**
     * 查未缴清费用学员列表
     */
    @Tool(description = """
            查询所有有待确认缴费（status=2）的学员列表。无参数。
            返回：每行 "学员名（班级） 费用类型 X元 日期"；无记录返回 "当前没有未缴清费用的学员"
            """)
    public String getUnpaidStudents() {
        List<Payment> list = paymentMapper.findByStatus(2);
        if (list == null || list.isEmpty()) return "当前没有未缴清费用的学员";
        StringBuilder sb = new StringBuilder("未缴清费用学员共" + list.size() + "条记录：\n");
        for (Payment p : list) {
            sb.append(p.getStudentName() != null ? p.getStudentName() : "学员" + p.getStudentId())
              .append("（").append(p.getClazzName() != null ? p.getClazzName() : "未知班级").append("）")
              .append(" ").append(p.getPaymentType())
              .append(" ").append(p.getAmount()).append("元")
              .append(" ").append(p.getPaymentDate())
              .append("\n");
        }
        return sb.toString();
    }

    // ========== 就业（2） ==========

    /**
     * 按班查就业率
     */
    @Tool(description = """
            查询指定班级的就业率统计。
            参数：clazzId（整数，必填）
            返回：班级总人数/已就业/就业率/平均+最高+最低薪资；无记录返回 "暂无就业记录"
            """)
    public String getClassEmploymentRate(Integer clazzId) {
        Clazz clazz = clazzMapper.getById(clazzId);
        String clazzName = clazz != null ? clazz.getName() : "ID=" + clazzId;
        Map<String, Object> stats = employmentMapper.getEmploymentStats(clazzId);
        if (stats == null || stats.get("employed_count") == null || ((Number) stats.get("employed_count")).longValue() == 0) {
            List<Student> students = studentMapper.pageList(new StudentQueryParam() {{ setClazzId(clazzId); setPageSize(100); }});
            return clazzName + "就业统计：暂无就业记录，班级共" + students.size() + "人";
        }
        long employedCount = ((Number) stats.get("employed_count")).longValue();
        List<Student> students = studentMapper.pageList(new StudentQueryParam() {{ setClazzId(clazzId); setPageSize(100); }});
        double rate = students.size() > 0 ? (employedCount * 100.0 / students.size()) : 0;
        return clazzName + "就业统计：\n"
                + "班级" + students.size() + "人，已就业" + employedCount + "人，就业率" + String.format("%.1f%%", rate) + "\n"
                + "平均薪资" + String.format("%.0f", ((Number) stats.get("avg_salary")).doubleValue()) + "元"
                + "，最高" + stats.get("max_salary") + "元，最低" + stats.get("min_salary") + "元";
    }

    /**
     * 按学员查就业信息
     */
    @Tool(description = """
            查询指定学员的就业信息。
            参数：studentId（整数，必填）
            返回：公司/职位/薪资/城市/就业日期/状态（在职/离职/试用期）；无记录返回 "该学员暂无就业记录"
            """)
    public String getStudentEmployment(Integer studentId) {
        Employment em = employmentMapper.findByStudentId(studentId);
        if (em == null) return "该学员暂无就业记录";
        Student s = studentMapper.getById(studentId);
        String name = s != null ? s.getName() : "ID=" + studentId;
        return name + "的就业信息：\n"
                + "公司：" + em.getCompany() + "\n"
                + "职位：" + em.getPosition() + "\n"
                + "薪资：" + em.getSalary() + "元/月\n"
                + "城市：" + (em.getCity() != null ? em.getCity() : "未知") + "\n"
                + "就业日期：" + em.getEmploymentDate() + "\n"
                + "状态：" + (em.getStatus() == 1 ? "在职" : em.getStatus() == 2 ? "已离职" : "试用期");
    }

    private String formatAttendStatus(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 1 -> "正常";
            case 2 -> "迟到";
            case 3 -> "早退";
            case 4 -> "请假";
            case 5 -> "旷课";
            default -> "未知";
        };
    }
}
