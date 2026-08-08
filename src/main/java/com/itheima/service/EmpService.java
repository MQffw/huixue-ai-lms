package com.itheima.service;

import com.itheima.pojo.Emp;
import com.itheima.pojo.EmpQueryParam;
import com.itheima.pojo.LoginInfo;
import com.itheima.pojo.PageResult;

import java.util.List;

/**
 * 员工服务接口
 */
public interface EmpService {
    
    /**
     * 员工登录
     */
    LoginInfo login(Emp emp);
    
    /**
     * 分页查询
     */
    PageResult<Emp> page(EmpQueryParam param);
    
    /**
     * 新增员工
     */
    void save(Emp emp);
    
    /**
     * 批量删除
     */
    void delete(List<Integer> ids);
    
    /**
     * 根据ID查询
     */
    Emp getInfo(Integer id);
    
    /**
     * 更新员工
     */
    void update(Emp emp);
    
    /**
     * 查询所有员工（用于下拉框）
     */
    List<Emp> findAll();

    /**
     * 修改密码
     */
    void updatePassword(Integer id, String oldPassword, String newPassword);
}
