package com.itheima.service.impl;

import com.itheima.mapper.EmpLogMapper;
import com.itheima.pojo.EmpLog;
import com.itheima.pojo.PageResult;
import com.itheima.service.EmpLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmpLogServiceImpl implements EmpLogService {

    @Autowired
    private EmpLogMapper empLogMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)//需要在一个新的事务中运行
    @Override
    public void insertLog(EmpLog empLog) {
        empLogMapper.insert(empLog);
    }

    @Override
    public PageResult<EmpLog> page(Integer page, Integer pageSize) {
        // 设置默认值
        if (page == null) page = 1;
        if (pageSize == null) pageSize = 10;

        //1. 查询总记录数
        Long total = empLogMapper.count();
        //2. 计算分页起始位置 start
        Integer start = (page - 1) * pageSize;
        //3. 查询当前页数据列表
        List<EmpLog> rows = empLogMapper.pageList(start, pageSize);
        //4. 封装分页结果返回
        return new PageResult<>(total, rows);
    }
}
