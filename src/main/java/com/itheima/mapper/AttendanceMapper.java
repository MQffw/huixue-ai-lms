package com.itheima.mapper;

import com.itheima.pojo.Attendance;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface AttendanceMapper {

    @Select("""
        SELECT a.id, a.student_id, a.clazz_id, a.attend_date, a.status, a.remark,
               a.record_emp_id, a.create_time, s.name AS student_name, c.name AS clazz_name
        FROM attendance a
        LEFT JOIN student s ON a.student_id = s.id
        LEFT JOIN clazz c ON a.clazz_id = c.id
        WHERE a.student_id = #{studentId}
        ORDER BY a.attend_date DESC
        """)
    List<Attendance> findByStudentId(Integer studentId);

    @Select("""
        SELECT a.id, a.student_id, a.clazz_id, a.attend_date, a.status, a.remark,
               a.record_emp_id, a.create_time, s.name AS student_name, c.name AS clazz_name
        FROM attendance a
        LEFT JOIN student s ON a.student_id = s.id
        LEFT JOIN clazz c ON a.clazz_id = c.id
        WHERE a.student_id = #{studentId} AND a.attend_date = #{date}
        """)
    Attendance findByStudentIdAndDate(@Param("studentId") Integer studentId, @Param("date") LocalDate date);

    @Select("""
        SELECT a.id, a.student_id, a.clazz_id, a.attend_date, a.status, a.remark,
               a.record_emp_id, a.create_time, s.name AS student_name, c.name AS clazz_name
        FROM attendance a
        LEFT JOIN student s ON a.student_id = s.id
        LEFT JOIN clazz c ON a.clazz_id = c.id
        WHERE a.clazz_id = #{clazzId} AND a.attend_date = #{date}
        ORDER BY a.status, s.name
        """)
    List<Attendance> findByClazzIdAndDate(@Param("clazzId") Integer clazzId, @Param("date") LocalDate date);

    @Select("""
        SELECT a.status, COUNT(*) AS count
        FROM attendance a
        WHERE a.clazz_id = #{clazzId} AND a.attend_date = #{date}
        GROUP BY a.status
        """)
    List<Map<String, Object>> countByStatus(@Param("clazzId") Integer clazzId, @Param("date") LocalDate date);

    @Select("""
        SELECT a.status, COUNT(*) AS count
        FROM attendance a
        WHERE a.clazz_id = #{clazzId} AND a.attend_date BETWEEN #{startDate} AND #{endDate}
        GROUP BY a.status
        """)
    List<Map<String, Object>> countByStatusBetween(@Param("clazzId") Integer clazzId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    /** 加载全量考勤（含学生名+班级名），用于分页过滤 */
    @Select("""
        SELECT a.id, a.student_id, a.clazz_id, a.attend_date, a.status, a.remark,
               a.record_emp_id, a.create_time, s.name AS student_name, c.name AS clazz_name
        FROM attendance a
        LEFT JOIN student s ON a.student_id = s.id
        LEFT JOIN clazz c ON a.clazz_id = c.id
        ORDER BY a.attend_date DESC, a.id DESC
        """)
    List<Attendance> findAllWithNames();

    /** 分页查询（SQL 层过滤 + 分页，替代全量加载内存过滤） */
    @Select("""
        <script>
        SELECT a.id, a.student_id, a.clazz_id, a.attend_date, a.status, a.remark,
               a.record_emp_id, a.create_time, s.name AS student_name, c.name AS clazz_name
        FROM attendance a
        LEFT JOIN student s ON a.student_id = s.id
        LEFT JOIN clazz c ON a.clazz_id = c.id
        <where>
            <if test="clazzId != null"> AND a.clazz_id = #{clazzId} </if>
            <if test="status != null"> AND a.status = #{status} </if>
            <if test="studentName != null and studentName != ''"> AND s.name LIKE CONCAT('%', #{studentName}, '%') </if>
            <if test="startDate != null"> AND a.attend_date &gt;= #{startDate} </if>
            <if test="endDate != null"> AND a.attend_date &lt;= #{endDate} </if>
        </where>
        ORDER BY a.attend_date DESC, a.id DESC
        LIMIT #{limit} OFFSET #{offset}
        </script>
        """)
    List<Attendance> findPage(@Param("clazzId") Integer clazzId,
                              @Param("status") Integer status,
                              @Param("studentName") String studentName,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate,
                              @Param("offset") int offset,
                              @Param("limit") int limit);

    /** 分页查询总数 */
    @Select("""
        <script>
        SELECT COUNT(*)
        FROM attendance a
        LEFT JOIN student s ON a.student_id = s.id
        <where>
            <if test="clazzId != null"> AND a.clazz_id = #{clazzId} </if>
            <if test="status != null"> AND a.status = #{status} </if>
            <if test="studentName != null and studentName != ''"> AND s.name LIKE CONCAT('%', #{studentName}, '%') </if>
            <if test="startDate != null"> AND a.attend_date &gt;= #{startDate} </if>
            <if test="endDate != null"> AND a.attend_date &lt;= #{endDate} </if>
        </where>
        </script>
        """)
    long countByCondition(@Param("clazzId") Integer clazzId,
                          @Param("status") Integer status,
                          @Param("studentName") String studentName,
                          @Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate);

    @Insert("INSERT INTO attendance (student_id, clazz_id, attend_date, status, remark, record_emp_id, create_time) " +
            "VALUES (#{studentId}, #{clazzId}, #{attendDate}, #{status}, #{remark}, #{recordEmpId}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Attendance a);

    @Update("UPDATE attendance SET student_id=#{studentId}, clazz_id=#{clazzId}, attend_date=#{attendDate}, status=#{status}, remark=#{remark} WHERE id=#{id}")
    int update(Attendance a);

    @Delete("DELETE FROM attendance WHERE id = #{id}")
    int deleteById(Integer id);

    @Select("""
        SELECT a.id, a.student_id, a.clazz_id, a.attend_date, a.status, a.remark,
               a.record_emp_id, a.create_time, s.name AS student_name, c.name AS clazz_name
        FROM attendance a
        LEFT JOIN student s ON a.student_id = s.id
        LEFT JOIN clazz c ON a.clazz_id = c.id
        WHERE a.attend_date = #{date} AND a.status != 1
        ORDER BY a.status, c.name, s.name
        """)
    List<Attendance> findAbnormalByDate(@Param("date") LocalDate date);
}
