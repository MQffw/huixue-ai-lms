package com.itheima.mapper;

import com.itheima.pojo.EmpLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmpLogMapper {

    @Insert("INSERT INTO emp_log (operate_emp_id, operate_time, class_name, method_name, method_params, return_value, cost_time) " +
            "VALUES (#{operateEmpId}, #{operateTime}, #{className}, #{methodName}, #{methodParams}, #{returnValue}, #{costTime})")
    public void insert(EmpLog empLog);

    // 分页查询日志列表
    List<EmpLog> pageList(@Param("start") Integer start, @Param("pageSize") Integer pageSize);

    // 统计日志总数
    Long count();

}
