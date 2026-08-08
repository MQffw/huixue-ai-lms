package com.itheima.service.impl;

import com.itheima.mapper.EmpLogMapper;
import com.itheima.pojo.EmpLog;
import com.itheima.pojo.PageResult;
import com.itheima.service.EmpLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class EmpLogServiceImpl implements EmpLogService {
    
    @Autowired
    private EmpLogMapper empLogMapper;
    
    @Override
    public void insertLog(EmpLog log) {
        empLogMapper.insert(log);
    }
    
    @Override
    public PageResult<EmpLog> page(Integer page, Integer pageSize) {
        Long total = empLogMapper.count();
        Integer offset = (page - 1) * pageSize;
        List<EmpLog> list = empLogMapper.page(offset, pageSize);
        return new PageResult<>(total, list);
    }
}
