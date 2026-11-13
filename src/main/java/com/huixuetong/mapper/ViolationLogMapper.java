package com.huixuetong.mapper;

import com.huixuetong.pojo.ViolationLog;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ViolationLogMapper {

    @Select("""
        SELECT vl.id, vl.student_id, vl.violation_type, vl.violation_date, vl.deduct_score,
               vl.description, vl.handler_id, vl.create_time,
               s.name AS student_name, e.name AS handler_name
        FROM violation_log vl
        LEFT JOIN student s ON vl.student_id = s.id
        LEFT JOIN emp e ON vl.handler_id = e.id
        WHERE vl.student_id = #{studentId}
        ORDER BY vl.violation_date DESC
        """)
    List<ViolationLog> findByStudentId(Integer studentId);

    @Select("""
        SELECT vl.id, vl.student_id, vl.violation_type, vl.violation_date, vl.deduct_score,
               vl.description, vl.handler_id, vl.create_time,
               s.name AS student_name, e.name AS handler_name
        FROM violation_log vl
        LEFT JOIN student s ON vl.student_id = s.id
        LEFT JOIN emp e ON vl.handler_id = e.id
        WHERE vl.violation_date BETWEEN #{startDate} AND #{endDate}
        ORDER BY vl.violation_date DESC
        """)
    List<ViolationLog> findRecent(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("""
            <script>
            SELECT vl.id, vl.student_id, vl.violation_type, vl.violation_date, vl.deduct_score,
                   vl.description, vl.handler_id, vl.create_time,
                   s.name AS student_name, e.name AS handler_name
            FROM violation_log vl
            LEFT JOIN student s ON vl.student_id = s.id
            LEFT JOIN emp e ON vl.handler_id = e.id
            <where>
                <if test="startDate != null and startDate != ''"> AND vl.violation_date &gt;= #{startDate} </if>
                <if test="endDate != null and endDate != ''"> AND vl.violation_date &lt;= #{endDate} </if>
                <if test="studentName != null and studentName != ''"> AND s.name LIKE CONCAT('%', #{studentName}, '%') </if>
                <if test="violationType != null and violationType != ''"> AND vl.violation_type = #{violationType} </if>
            </where>
            ORDER BY vl.violation_date DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<ViolationLog> findPage(@Param("startDate") String startDate,
                                @Param("endDate") String endDate,
                                @Param("studentName") String studentName,
                                @Param("violationType") String violationType,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(*) FROM violation_log vl LEFT JOIN student s ON vl.student_id = s.id
            <where>
                <if test="startDate != null and startDate != ''"> AND vl.violation_date &gt;= #{startDate} </if>
                <if test="endDate != null and endDate != ''"> AND vl.violation_date &lt;= #{endDate} </if>
                <if test="studentName != null and studentName != ''"> AND s.name LIKE CONCAT('%', #{studentName}, '%') </if>
                <if test="violationType != null and violationType != ''"> AND vl.violation_type = #{violationType} </if>
            </where>
            </script>
            """)
    long countPage(@Param("startDate") String startDate,
                   @Param("endDate") String endDate,
                   @Param("studentName") String studentName,
                   @Param("violationType") String violationType);

    @Select("""
        SELECT vl.violation_type, COUNT(*) AS count
        FROM violation_log vl
        WHERE vl.violation_date BETWEEN #{startDate} AND #{endDate}
        GROUP BY vl.violation_type
        ORDER BY count DESC
        """)
    List<Map<String, Object>> countByType(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("""
        SELECT s.name AS student_name, COUNT(*) AS violation_count, SUM(vl.deduct_score) AS total_deduct
        FROM violation_log vl
        LEFT JOIN student s ON vl.student_id = s.id
        WHERE vl.violation_date BETWEEN #{startDate} AND #{endDate}
        GROUP BY vl.student_id, s.name
        ORDER BY total_deduct DESC
        LIMIT #{limit}
        """)
    List<Map<String, Object>> findTopViolators(@Param("startDate") String startDate,
                                                @Param("endDate") String endDate,
                                                @Param("limit") int limit);

    @Insert("INSERT INTO violation_log (student_id, violation_type, violation_date, deduct_score, description, handler_id, create_time) " +
            "VALUES (#{studentId}, #{violationType}, #{violationDate}, #{deductScore}, #{description}, #{handlerId}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ViolationLog v);

    @Update("UPDATE violation_log SET student_id=#{studentId}, violation_type=#{violationType}, violation_date=#{violationDate}, " +
            "deduct_score=#{deductScore}, description=#{description}, handler_id=#{handlerId} WHERE id=#{id}")
    int update(ViolationLog v);

    @Delete("DELETE FROM violation_log WHERE id = #{id}")
    int deleteById(Integer id);
}
