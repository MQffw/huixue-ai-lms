package com.itheima.service.impl;

import com.itheima.ai.cache.AiAnswerCache;
import com.itheima.mapper.DeptMapper;
import com.itheima.pojo.Dept;
import com.itheima.service.DeptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class DeptServiceImpl implements DeptService {
    
    @Autowired
    private DeptMapper deptMapper;

    @Autowired
    private AiAnswerCache aiAnswerCache;
    
    @Override
    public List<Dept> finAll() {
        return deptMapper.findAll();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Integer id) {
        try {
            Dept dept = deptMapper.getById(id);
            if (dept == null) {
                throw new RuntimeException("部门不存在: id=" + id);
            }

            deptMapper.deleteById(id);
            log.info("删除部门成功: id={}, name={}", id, dept.getName());
            aiAnswerCache.clear();

        } catch (Exception e) {
            log.error("删除部门失败: id={}", id, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Integer> ids) {
        try {
            deptMapper.deleteByIds(ids);
            log.info("批量删除部门成功: count={}", ids.size());
            aiAnswerCache.clear();
        } catch (Exception e) {
            log.error("批量删除部门失败: count={}", ids.size(), e);
            throw e;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Dept dept) {
        try {
            dept.setCreateTime(LocalDateTime.now());
            dept.setUpdateTime(LocalDateTime.now());
            deptMapper.insert(dept);
            log.info("新增部门成功: id={}, name={}", dept.getId(), dept.getName());
            aiAnswerCache.clear();
            
        } catch (Exception e) {
            log.error("新增部门失败: {}", dept.getName(), e);
            throw e;
        }
    }
    
    @Override
    public Dept getById(Integer id) {
        return deptMapper.getById(id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Dept dept) {
        try {
            dept.setUpdateTime(LocalDateTime.now());
            deptMapper.update(dept);
            log.info("修改部门成功: id={}, name={}", dept.getId(), dept.getName());
            aiAnswerCache.clear();
            
        } catch (Exception e) {
            log.error("修改部门失败: id={}", dept.getId(), e);
            throw e;
        }
    }
}
