package com.itheima.mapper;

import com.itheima.pojo.Dept;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DeptMapper {
//查
    @Select("SELECT id,name,create_time,update_time FROM dept ORDER BY update_time desc")
    List<Dept> findAll();
//删
    @Delete("delete from dept where id = #{id}")
    void deleteById(Integer id);
//批量删除
    @Delete("<script>DELETE FROM dept WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    void deleteByIds(@org.apache.ibatis.annotations.Param("ids") List<Integer> ids);
//增
    @Insert("insert into dept(name,create_time,update_time) values(#{name},#{createTime},#{updateTime}) ")
    void insert(Dept dept);
//先回显再改
    @Select("select id,name,create_time,update_time from dept where id = #{id}")
    Dept getById(Integer id);

    @Update("update dept set name = #{name}, update_time = #{updateTime} where id = #{id} ")
    void update(Dept dept);
}
