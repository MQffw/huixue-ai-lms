package com.huixuetong.service;

import com.huixuetong.pojo.EmpLog;
import com.huixuetong.pojo.PageResult;

public interface EmpLogService {

    public void insertLog(EmpLog empLog);

    // 分页查询日志
    PageResult<EmpLog> page(Integer page, Integer pageSize);

}
