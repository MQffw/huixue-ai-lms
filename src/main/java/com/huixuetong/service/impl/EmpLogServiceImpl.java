package com.huixuetong.service.impl;

import com.huixuetong.mapper.EmpLogMapper;
import com.huixuetong.pojo.EmpLog;
import com.huixuetong.pojo.PageResult;
import com.huixuetong.service.EmpLogService;
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
