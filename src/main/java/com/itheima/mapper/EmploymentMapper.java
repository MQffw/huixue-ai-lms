package com.itheima.mapper;

import com.itheima.pojo.Employment;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface EmploymentMapper {

    @Select("""
        SELECT em.id, em.student_id, em.clazz_id, em.company, em.position,
               em.salary, em.city, em.employment_date, em.status, em.create_time,
               s.name AS student_name, c.name AS clazz_name
        FROM employment em
        LEFT JOIN student s ON em.student_id = s.id
        LEFT JOIN clazz c ON em.clazz_id = c.id
        WHERE em.student_id = #{studentId}
        """)
    Employment findByStudentId(Integer studentId);

    @Select("""
        SELECT em.id, em.student_id, em.clazz_id, em.company, em.position,
               em.salary, em.city, em.employment_date, em.status, em.create_time,
               s.name AS student_name, c.name AS clazz_name
        FROM employment em
        LEFT JOIN student s ON em.student_id = s.id
        LEFT JOIN clazz c ON em.clazz_id = c.id
        WHERE em.clazz_id = #{clazzId}
        ORDER BY em.salary DESC
        """)
    List<Employment> findByClazzId(Integer clazzId);

    @Select("""
        SELECT em.id, em.student_id, em.clazz_id, em.company, em.position,
               em.salary, em.city, em.employment_date, em.status, em.create_time,
               s.name AS student_name, c.name AS clazz_name
        FROM employment em
        LEFT JOIN student s ON em.student_id = s.id
        LEFT JOIN clazz c ON em.clazz_id = c.id
        ORDER BY em.salary DESC
        """)
    List<Employment> findAll();

    @Select("""
        SELECT COUNT(*) AS employed_count, AVG(em.salary) AS avg_salary,
               MAX(em.salary) AS max_salary, MIN(em.salary) AS min_salary
        FROM employment em
        WHERE em.clazz_id = #{clazzId}
        """)
    Map<String, Object> getEmploymentStats(Integer clazzId);

    @Select("""
        SELECT c.name AS clazz_name,
               COUNT(em.id) AS employed_count,
               AVG(em.salary) AS avg_salary,
               MAX(em.salary) AS max_salary
        FROM employment em
        LEFT JOIN clazz c ON em.clazz_id = c.id
        GROUP BY em.clazz_id, c.name
        ORDER BY avg_salary DESC
        """)
    List<Map<String, Object>> getEmploymentStatsByClazz();

    @Insert("INSERT INTO employment (student_id, clazz_id, company, position, salary, city, employment_date, status, create_time) " +
            "VALUES (#{studentId}, #{clazzId}, #{company}, #{position}, #{salary}, #{city}, #{employmentDate}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Employment e);

    @Update("UPDATE employment SET student_id=#{studentId}, clazz_id=#{clazzId}, company=#{company}, position=#{position}, " +
            "salary=#{salary}, city=#{city}, employment_date=#{employmentDate}, status=#{status} WHERE id=#{id}")
    int update(Employment e);

    @Delete("DELETE FROM employment WHERE id = #{id}")
    int deleteById(Integer id);
}
