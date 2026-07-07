package com.itheima.service;

import com.itheima.pojo.Dept;

import java.util.List;

public interface DeptService {
    /**
     * 查询所有部门
     * @return
     */
    List<Dept> finAll();
    /**
     * 根据id删除部门
     * @return
     */
    void deleteById(Integer id);
    /**
     * 批量删除部门
     */
    void deleteByIds(List<Integer> ids);
    /**
     * 新增部门
     * @return
     */
    void add(Dept dept);
    /**
     * 根据id查询部门
     * @return
     */
    Dept getById(Integer id);
    /**
     * 修改部门
     * @return
     */

    void update(Dept dept);
}
