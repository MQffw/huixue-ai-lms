package com.itheima.mapper;

import com.itheima.pojo.Score;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ScoreMapper {

    @Select("""
        SELECT sc.id, sc.exam_id, sc.student_id, sc.score, sc.rank, sc.remark, sc.create_time,
               s.name AS student_name, e.name AS exam_name
        FROM score sc
        LEFT JOIN student s ON sc.student_id = s.id
        LEFT JOIN exam e ON sc.exam_id = e.id
        WHERE sc.student_id = #{studentId}
        ORDER BY e.exam_date DESC
        """)
    List<Score> findByStudentId(Integer studentId);

    @Select("""
        SELECT sc.id, sc.exam_id, sc.student_id, sc.score, sc.rank, sc.remark, sc.create_time,
               s.name AS student_name, e.name AS exam_name
        FROM score sc
        LEFT JOIN student s ON sc.student_id = s.id
        LEFT JOIN exam e ON sc.exam_id = e.id
        WHERE sc.exam_id = #{examId}
        ORDER BY sc.score DESC
        """)
    List<Score> findByExamId(Integer examId);

    @Select("""
        SELECT AVG(sc.score) AS avg_score, MAX(sc.score) AS max_score,
               MIN(sc.score) AS min_score, COUNT(*) AS total_count,
               SUM(CASE WHEN sc.score < e.pass_score THEN 1 ELSE 0 END) AS fail_count
        FROM score sc
        JOIN exam e ON sc.exam_id = e.id
        WHERE sc.exam_id = #{examId}
        """)
    Map<String, Object> getExamStats(Integer examId);

    @Select("""
        SELECT sc.id, sc.exam_id, sc.student_id, sc.score, sc.rank, sc.remark, sc.create_time,
               s.name AS student_name, e.name AS exam_name
        FROM score sc
        LEFT JOIN student s ON sc.student_id = s.id
        LEFT JOIN exam e ON sc.exam_id = e.id
        WHERE sc.exam_id = #{examId} AND sc.score < #{passScore}
        ORDER BY sc.score
        """)
    List<Score> findFailingByExamId(@Param("examId") Integer examId, @Param("passScore") Integer passScore);

    @Insert("INSERT INTO score (exam_id, student_id, score, `rank`, remark, create_time) VALUES (#{examId}, #{studentId}, #{score}, #{rank}, #{remark}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Score s);

    @Update("UPDATE score SET exam_id=#{examId}, student_id=#{studentId}, score=#{score}, `rank`=#{rank}, remark=#{remark} WHERE id=#{id}")
    int update(Score s);

    @Delete("DELETE FROM score WHERE id = #{id}")
    int deleteById(Integer id);

    /** 查询全部成绩（含学生名+考试名），用于无 examId 时全量查询 */
    @Select("""
        SELECT sc.id, sc.exam_id, sc.student_id, sc.score, sc.rank, sc.remark, sc.create_time,
               s.name AS student_name, e.name AS exam_name
        FROM score sc
        LEFT JOIN student s ON sc.student_id = s.id
        LEFT JOIN exam e ON sc.exam_id = e.id
        ORDER BY sc.create_time DESC
        """)
    List<Score> findAllWithNames();

    /** 批量删除成绩 */
    @Delete("<script> DELETE FROM score WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            " </script>")
    int deleteByIds(@Param("ids") List<Integer> ids);
}
