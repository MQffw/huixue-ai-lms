package com.itheima.mapper;

import com.itheima.pojo.EmpLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 员工操作日志Mapper
 */
@Mapper
public interface EmpLogMapper {

    /**
     * 插入操作日志
     */
    @Insert("INSERT INTO emp_log (operate_emp_id, class_name, method_name, method_params, return_value, operate_time, cost_time) " +
            "VALUES (#{operateEmpId}, #{className}, #{methodName}, #{methodParams}, #{returnValue}, #{operateTime}, #{costTime})")
    int insert(EmpLog log);

    /**
     * 分页查询操作日志（关联emp表获取操作人姓名）
     */
    @Select("SELECT l.*, e.name AS operate_emp_name FROM emp_log l LEFT JOIN emp e ON l.operate_emp_id = e.id ORDER BY l.operate_time DESC LIMIT #{offset}, #{pageSize}")
    List<EmpLog> page(@org.apache.ibatis.annotations.Param("offset") Integer offset, @org.apache.ibatis.annotations.Param("pageSize") Integer pageSize);

    /**
     * 查询操作日志总数
     */
    @Select("SELECT COUNT(*) FROM emp_log")
    Long count();
}
