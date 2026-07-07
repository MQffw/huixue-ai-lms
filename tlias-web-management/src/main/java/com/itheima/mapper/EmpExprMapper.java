package com.itheima.mapper;

import com.itheima.pojo.EmpExpr;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface EmpExprMapper {

    /**
     * 批量保存员工工作经历
     */
    void insertBatch(List<EmpExpr> exprList);

    /**
     * 根据员工id批量删除员工工作经历
     */
    void deleteByEmpIds(List<Integer> empIds);

    /**
     * 查询指定员工的工作经历
     */
    List<EmpExpr> findByEmpId(Integer empId);

    /**
     * 查询有工作经历记录的所有员工ID（去重）
     */
    List<Integer> findEmpIdsWithExpr();

    /**
     * 查询所有工作经历（关联员工姓名）
     */
    List<Map<String, Object>> findAllWithEmpName();
}
