package com.itheima.service.impl;

import com.itheima.ai.cache.AiAnswerCache;
import com.itheima.mapper.ClazzMapper;
import com.itheima.mapper.CourseScheduleMapper;
import com.itheima.pojo.Clazz;
import com.itheima.pojo.ClazzQueryParam;
import com.itheima.pojo.PageResult;
import com.itheima.service.ClazzService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class ClazzServiceImpl implements ClazzService {

    @Autowired
    private ClazzMapper clazzMapper;
    @Autowired
    private CourseScheduleMapper courseScheduleMapper;
    @Autowired
    private AiAnswerCache aiAnswerCache;

    @Override
    public PageResult<Clazz> page(ClazzQueryParam param) {
        Long total = clazzMapper.count(param);
        List<Clazz> rows = clazzMapper.pageList(param);
        return new PageResult<>(total, rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Integer id) {
        courseScheduleMapper.deleteByClazzId(id);  // 级联删除排课记录
        clazzMapper.deleteById(id);
        aiAnswerCache.clear();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Integer> ids) {
        try {
            for (Integer id : ids) {
                courseScheduleMapper.deleteByClazzId(id);  // 级联删除排课
            }
            clazzMapper.deleteByIds(ids);
            log.info("批量删除班级成功: count={}", ids.size());
            aiAnswerCache.clear();
        } catch (Exception e) {
            log.error("批量删除班级失败: count={}", ids.size(), e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Clazz clazz) {
        clazzMapper.insert(clazz);
        aiAnswerCache.clear();
    }

    @Override
    public Clazz getById(Integer id) {
        return clazzMapper.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Clazz clazz) {
        clazzMapper.updateById(clazz);
        aiAnswerCache.clear();
    }

    @Override
    public List<Clazz> listAll() {
        return clazzMapper.listAll();
    }
}
