package com.itheima.mapper;

import com.itheima.pojo.Emp;
import com.itheima.pojo.EmpQueryParam;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface EmpMapper {
    
    Emp findByUsername(String username);
    List<Emp> pageList(EmpQueryParam param);
    Long count(EmpQueryParam param);
    int deleteByIds(@Param("ids") List<Integer> ids);
    
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Emp emp);
    
    Emp getById(Integer id);
    int updateById(Emp emp);
    List<Emp> findAll();
    
    List<Map<String, Object>> countEmpJobData();
    List<Map<String, Object>> countEmpGenderData();

    int updatePassword(@Param("id") Integer id, @Param("newPassword") String newPassword);
}
