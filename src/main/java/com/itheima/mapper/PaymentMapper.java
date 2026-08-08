package com.itheima.mapper;

import com.itheima.pojo.Payment;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface PaymentMapper {

    @Select("""
        SELECT p.id, p.student_id, p.amount, p.payment_type, p.payment_method,
               p.payment_date, p.status, p.operator_id, p.remark, p.create_time,
               s.name AS student_name, c.name AS clazz_name
        FROM payment p
        LEFT JOIN student s ON p.student_id = s.id
        LEFT JOIN clazz c ON s.clazz_id = c.id
        WHERE p.student_id = #{studentId}
        ORDER BY p.payment_date DESC
        """)
    List<Payment> findByStudentId(Integer studentId);

    @Select("""
        SELECT p.payment_type, SUM(p.amount) AS total_amount
        FROM payment p
        WHERE p.student_id = #{studentId} AND p.status = 1
        GROUP BY p.payment_type
        """)
    List<Map<String, Object>> sumByStudentAndType(Integer studentId);

    @Select("""
        SELECT p.id, p.student_id, p.amount, p.payment_type, p.payment_method,
               p.payment_date, p.status, p.operator_id, p.remark, p.create_time,
               s.name AS student_name, c.name AS clazz_name
        FROM payment p
        LEFT JOIN student s ON p.student_id = s.id
        LEFT JOIN clazz c ON s.clazz_id = c.id
        WHERE p.status = #{status}
        ORDER BY p.payment_date DESC
        """)
    List<Payment> findByStatus(Integer status);

    @Select("""
        SELECT p.payment_type, SUM(p.amount) AS total_amount, COUNT(DISTINCT p.student_id) AS student_count
        FROM payment p
        WHERE p.payment_date BETWEEN #{startDate} AND #{endDate} AND p.status = 1
        GROUP BY p.payment_type
        """)
    List<Map<String, Object>> sumByTypeBetween(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Insert("INSERT INTO payment (student_id, amount, payment_type, payment_method, payment_date, status, operator_id, remark, create_time) " +
            "VALUES (#{studentId}, #{amount}, #{paymentType}, #{paymentMethod}, #{paymentDate}, #{status}, #{operatorId}, #{remark}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Payment p);

    @Update("UPDATE payment SET student_id=#{studentId}, amount=#{amount}, payment_type=#{paymentType}, payment_method=#{paymentMethod}, " +
            "payment_date=#{paymentDate}, status=#{status}, operator_id=#{operatorId}, remark=#{remark} WHERE id=#{id}")
    int update(Payment p);

    @Delete("DELETE FROM payment WHERE id = #{id}")
    int deleteById(Integer id);
}
