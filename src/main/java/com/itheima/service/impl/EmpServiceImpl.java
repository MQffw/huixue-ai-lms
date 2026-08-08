package com.itheima.service.impl;

import com.itheima.ai.cache.AiAnswerCache;
import com.itheima.mapper.EmpMapper;
import com.itheima.pojo.*;
import com.itheima.service.EmpService;
import com.itheima.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmpServiceImpl implements EmpService {
    
    @Autowired
    private EmpMapper empMapper;

    @Autowired
    private AiAnswerCache aiAnswerCache;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Override
    public LoginInfo login(Emp emp) {
        Emp dbEmp = empMapper.findByUsername(emp.getUsername());
        
        if (dbEmp == null || !dbEmp.getPassword().equals(emp.getPassword())) {
            return null;
        }
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", dbEmp.getId());
        claims.put("username", dbEmp.getUsername());
        claims.put("role", "ADMIN");
        
        String token = jwtUtils.generateToken(claims);
        
        return new LoginInfo(dbEmp.getId(), dbEmp.getUsername(), dbEmp.getName(), token);
    }
    
    @Override
    public PageResult<Emp> page(EmpQueryParam param) {
        Long count = empMapper.count(param);
        List<Emp> rows = empMapper.pageList(param);
        return new PageResult<>(count, rows);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Emp emp) {
        try {
            emp.setCreateTime(LocalDateTime.now());
            emp.setUpdateTime(LocalDateTime.now());
            empMapper.insert(emp);
            log.info("新增员工成功: id={}, username={}", emp.getId(), emp.getUsername());
            aiAnswerCache.clear();
        } catch (Exception e) {
            log.error("新增员工失败: username={}", emp.getUsername(), e);
            throw e;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Integer> ids) {
        try {
            int count = empMapper.deleteByIds(ids);
            log.info("批量删除员工成功: count={}", count);
            aiAnswerCache.clear();
        } catch (Exception e) {
            log.error("批量删除员工失败: count={}", ids.size(), e);
            throw e;
        }
    }
    
    @Override
    public Emp getInfo(Integer id) {
        return empMapper.getById(id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Emp emp) {
        try {
            emp.setUpdateTime(LocalDateTime.now());
            empMapper.updateById(emp);
            log.info("修改员工成功: id={}, username={}", emp.getId(), emp.getUsername());
            aiAnswerCache.clear();
        } catch (Exception e) {
            log.error("修改员工失败: id={}", emp.getId(), e);
            throw e;
        }
    }
    
    @Override
    public List<Emp> findAll() {
        return empMapper.findAll();
    }

    @Override
    public void updatePassword(Integer id, String oldPassword, String newPassword) {
        Emp emp = empMapper.getById(id);
        if (emp == null) {
            throw new RuntimeException("\u7528\u6237\u4e0d\u5b58\u5728");
        }
        if (!emp.getPassword().equals(oldPassword)) {
            throw new RuntimeException("\u65e7\u5bc6\u7801\u9519\u8bef");
        }
        empMapper.updatePassword(id, newPassword);
        log.info("\u4fee\u6539\u5bc6\u7801\u6210\u529f: id={}", id);
    }
}
