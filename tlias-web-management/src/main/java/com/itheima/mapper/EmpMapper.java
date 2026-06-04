package com.itheima.mapper;

import com.itheima.pojo.Emp;
import com.itheima.pojo.EmpQueryParam;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Mapper
public interface EmpMapper {
    //查询总记录数返回给前端
    @Select("select count(*) from emp e left join dept d on e.dept_id = d.id")
    public Long count();

    //分页查询加排序，根据更新时间倒叙
    public List<Emp> list(EmpQueryParam empQueryParam);


    /**
     *新增员工基本信息
     */
    @Options(useGeneratedKeys = true,keyProperty = "id")//mybatis里主键返回
    @Insert("INSERT into emp(username,name,gender,phone,job,salary,image,entry_date,dept_id,create_time,update_time) " +
            "VALUES(#{username},#{name},#{gender},#{phone},#{job},#{salary},#{image},#{entryDate},#{deptId},#{createTime},#{updateTime})")
    void insert(Emp emp);

    /**
     *根据id批量删除员工信息
     */
    void deleteByIds(List<Integer> ids);

    /**
     *根据id查询员工信息以及工作经历信息
     */
    Emp getById(Integer id);

    /**
     *根据id更新员工信息以及工作经历信息
     */
    void updateById(Emp emp);

    /**
     *统计员工职位对应人数
     */
    List<Map<String, Object>> countEmpJobData();

    /**
     *统计员工性别对应人数
     */
    List<Map<String, Object>> countEmpGenderData();

    /**
     * 根据用户名和密码查询员工信息
     * @param emp
     * @return
     */
    @Select("select id,name,username from emp where username = #{username} and password = #{password}")
    Emp selectByUsernameAndPassword(Emp emp);
}
