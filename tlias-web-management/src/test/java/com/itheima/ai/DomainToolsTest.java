package com.itheima.ai;

import com.itheima.ai.tool.*;
import com.itheima.mapper.*;
import com.itheima.pojo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AI Agent 域 Tool 单元测试
 *
 * 直接注入各域 Tool Bean 进行测试（Mockito @InjectMocks）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AI Agent 域 Tool 方法")
class DomainToolsTest {

    // ===== 学员域 =====
    @InjectMocks private StudentTools studentTools;
    @Mock private StudentMapper studentMapper;
    @Mock private ClazzMapper clazzMapper;

    // ===== 员工域 =====
    @InjectMocks private EmployeeTools employeeTools;
    @Mock private EmpMapper empMapper;
    @Mock private EmpExprMapper empExprMapper;

    // ===== 班级域 =====
    @InjectMocks private ClazzTools clazzTools;
    @Mock private DeptMapper deptMapper;

    // ===== 课程域 =====
    @InjectMocks private CourseTools courseTools;
    @Mock private CourseMapper courseMapper;
    @Mock private CourseScheduleMapper scheduleMapper;

    // ===== 综合事务域 =====
    @InjectMocks private AffairsTools affairsTools;
    @Mock private AttendanceMapper attendanceMapper;
    @Mock private ScoreMapper scoreMapper;
    @Mock private ViolationLogMapper violationLogMapper;
    @Mock private PaymentMapper paymentMapper;
    @Mock private EmploymentMapper employmentMapper;

    // ===== 通知域 =====
    @InjectMocks private NoticeTools noticeTools;
    @Mock private NoticeMapper noticeMapper;
    @Mock private com.itheima.security.sql.SecureSqlExecutor secureSqlExecutor;
    @Mock private ExamMapper examMapper;

    // ==================== 学员工具 ====================

    @Test @DisplayName("getStudentById - 找到")
    void getStudentById_found() {
        Student s = new Student(); s.setId(1); s.setName("段誉"); s.setGender(1);
        when(studentMapper.getById(1)).thenReturn(s);
        String result = studentTools.getStudentById(1);
        assertTrue(result.contains("段誉"));
        assertTrue(result.contains("ID=1"));
    }

    @Test @DisplayName("getStudentById - 未找到")
    void getStudentById_notFound() {
        when(studentMapper.getById(999)).thenReturn(null);
        String result = studentTools.getStudentById(999);
        assertTrue(result.contains("未找到"));
    }

    @Test @DisplayName("countStudentsByDegree - 有数据")
    void countStudentsByDegree() {
        when(studentMapper.countStudentDegreeData()).thenReturn(List.of(
            java.util.Map.of("pos", "本科", "num", 356)
        ));
        String result = studentTools.countStudentsByDegree();
        assertTrue(result.contains("本科: 356人"));
    }

    // ==================== 员工工具 ====================

    @Test @DisplayName("getEmpById - 找到")
    void getEmpById_found() {
        Emp e = new Emp(); e.setId(2); e.setName("乔峰"); e.setJob(2);
        when(empMapper.getById(2)).thenReturn(e);
        String result = employeeTools.getEmpById(2);
        assertTrue(result.contains("乔峰"));
    }

    @Test @DisplayName("listAllEmps - 员工列表")
    void listAllEmps() {
        Emp e = new Emp(); e.setId(2); e.setName("虚竹");
        when(empMapper.findAll()).thenReturn(List.of(e));
        String result = employeeTools.listAllEmps();
        assertTrue(result.contains("虚竹"));
        assertTrue(result.contains("共1名员工"));
    }

    // ==================== 班级工具 ====================

    @Test @DisplayName("listAllClazz - 班级列表")
    void listAllClazz() {
        Clazz c = new Clazz(); c.setId(1); c.setName("Java高级班"); c.setSubject(1);
        when(clazzMapper.listAll()).thenReturn(List.of(c));
        String result = clazzTools.listAllClazz();
        assertTrue(result.contains("Java高级班"));
    }

    // ==================== 课程工具 ====================

    @Test @DisplayName("getCourseList - 课程列表")
    void getCourseList() {
        Course c = new Course(); c.setName("SpringBoot"); c.setSubject(1); c.setHours(120);
        when(courseMapper.findAll()).thenReturn(List.of(c));
        String result = courseTools.getCourseList();
        assertTrue(result.contains("SpringBoot"));
        assertTrue(result.contains("120课时"));
    }

    @Test @DisplayName("getClassSchedule - 排课")
    void getClassSchedule() {
        Clazz clazz = new Clazz(); clazz.setName("Java1班");
        when(clazzMapper.getById(1)).thenReturn(clazz);
        CourseSchedule cs = new CourseSchedule();
        cs.setClazzName("Java1班"); cs.setClassDate(LocalDate.now());
        cs.setStartTime(java.time.LocalTime.of(9, 0));
        cs.setEndTime(java.time.LocalTime.of(12, 0));
        cs.setCourseName("SpringBoot"); cs.setTeacherName("李老师"); cs.setRoom("A101");
        when(scheduleMapper.findByClazzId(1)).thenReturn(List.of(cs));
        String result = courseTools.getClassSchedule(1);
        assertTrue(result.contains("SpringBoot"));
        assertTrue(result.contains("A101"));
    }

    // ==================== 考勤工具 ====================

    @Test @DisplayName("getStudentAttendance - 查考勤")
    void getStudentAttendance() {
        Student s = new Student(); s.setName("段誉");
        when(studentMapper.getById(1)).thenReturn(s);
        Attendance a = new Attendance(); a.setAttendDate(LocalDate.now()); a.setStatus(1);
        when(attendanceMapper.findByStudentId(1)).thenReturn(List.of(a));
        String result = affairsTools.getStudentAttendance(1);
        assertTrue(result.contains("段誉"));
        assertTrue(result.contains("正常"));
    }

    // ==================== 成绩工具 ====================

    @Test @DisplayName("getStudentScores - 查成绩")
    void getStudentScores() {
        Student s = new Student(); s.setName("段誉");
        when(studentMapper.getById(1)).thenReturn(s);
        Score sc = new Score(); sc.setExamName("期中考试"); sc.setScore(java.math.BigDecimal.valueOf(90)); sc.setRank(1);
        when(scoreMapper.findByStudentId(1)).thenReturn(List.of(sc));
        String result = affairsTools.getStudentScores(1);
        assertTrue(result.contains("90分"));
        assertTrue(result.contains("第1名"));
    }

    // ==================== 违纪工具 ====================

    @Test @DisplayName("getStudentViolations - 违纪记录")
    void getStudentViolations() {
        Student s = new Student(); s.setName("段誉");
        when(studentMapper.getById(1)).thenReturn(s);
        ViolationLog vl = new ViolationLog(); vl.setViolationDate(LocalDate.of(2026, 1, 15));
        vl.setViolationType("迟到"); vl.setDeductScore(2);
        when(violationLogMapper.findByStudentId(1)).thenReturn(List.of(vl));
        String result = affairsTools.getStudentViolations(1);
        assertTrue(result.contains("扣2分"));
        assertTrue(result.contains("累计扣分：2"));
    }

    // ==================== 缴费工具 ====================

    @Test @DisplayName("getStudentPaymentStatus - 缴费状态")
    void getStudentPaymentStatus() {
        Student s = new Student(); s.setName("段誉");
        when(studentMapper.getById(1)).thenReturn(s);
        Payment p = new Payment(); p.setPaymentDate(LocalDate.of(2026, 1, 1)); p.setPaymentType("学费");
        p.setAmount(java.math.BigDecimal.valueOf(8000)); p.setPaymentMethod("微信"); p.setStatus(1);
        when(paymentMapper.findByStudentId(1)).thenReturn(List.of(p));
        String result = affairsTools.getStudentPaymentStatus(1);
        assertTrue(result.contains("微信"));
        assertTrue(result.contains("已确认缴费合计：8000元"));
    }

    // ==================== 就业工具 ====================

    @Test @DisplayName("getStudentEmployment - 就业信息")
    void getStudentEmployment() {
        Student s = new Student(); s.setName("段誉");
        when(studentMapper.getById(1)).thenReturn(s);
        Employment em = new Employment(); em.setCompany("阿里"); em.setPosition("工程师");
        em.setSalary(15000); em.setCity("杭州"); em.setEmploymentDate(LocalDate.of(2026, 3, 1)); em.setStatus(1);
        when(employmentMapper.findByStudentId(1)).thenReturn(em);
        String result = affairsTools.getStudentEmployment(1);
        assertTrue(result.contains("阿里"));
        assertTrue(result.contains("15000元/月"));
        assertTrue(result.contains("在职"));
    }

    @Test @DisplayName("getClassEmploymentRate - 就业率")
    void getClassEmploymentRate() {
        Clazz clazz = new Clazz(); clazz.setName("Java1班"); clazz.setId(1);
        when(clazzMapper.getById(1)).thenReturn(clazz);
        Student st = new Student(); st.setId(1); st.setClazzId(1);
        when(studentMapper.pageList(any())).thenReturn(List.of(st));
        when(employmentMapper.getEmploymentStats(1)).thenReturn(java.util.Map.of(
            "employed_count", 30, "avg_salary", 12000, "max_salary", 20000, "min_salary", 8000
        ));
        String result = affairsTools.getClassEmploymentRate(1);
        assertTrue(result.contains("就业率"));
    }

    // ==================== 通知工具 ====================

    @Test @DisplayName("searchNotice - 搜索通知")
    void searchNotice() {
        Notice n = new Notice(); n.setTitle("退费公示"); n.setContent("公示内容"); n.setType(2);
        when(noticeMapper.searchByKeyword("退费")).thenReturn(List.of(n));
        String result = noticeTools.searchNotice("退费");
        assertTrue(result.contains("退费公示"));
    }

    @Test @DisplayName("getLatestNotices - 最新通知")
    void getLatestNotices() {
        Notice n = new Notice(); n.setId(1); n.setTitle("公示"); n.setType(2);
        when(noticeMapper.findLatest(5)).thenReturn(List.of(n));
        String result = noticeTools.getLatestNotices(5);
        assertTrue(result.contains("公示"));
    }

    // ==================== executeQuery（安全兜底）====================

    @Test @DisplayName("executeQuery - 已禁用，返回安全限制")
    void executeQuery() {
        String result = noticeTools.executeQuery("SELECT * FROM emp LIMIT 1");
        assertTrue(result.contains("已禁用") || result.contains("安全限制"));
    }
}
