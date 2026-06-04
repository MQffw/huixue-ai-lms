package com.itheima.service.impl;

import com.itheima.mapper.ClazzMapper;
import com.itheima.pojo.Clazz;
import com.itheima.pojo.ClazzQueryParam;
import com.itheima.pojo.PageResult;
import com.itheima.service.ClazzService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClazzServiceImpl implements ClazzService {

    @Autowired
    private ClazzMapper clazzMapper;

    @Override
    public PageResult<Clazz> page(ClazzQueryParam param) {
        //1. 查询总记录数
        Long total = clazzMapper.count(param);
        //2. 计算分页起始位置 start
        Integer start = (param.getPage() - 1) * param.getPageSize();
        param.setStart(start);
        //3. 查询当前页数据列表
        List<Clazz> rows = clazzMapper.pageList(param);
        //4. 封装分页结果返回
        return new PageResult<>(total, rows);
    }

    @Override
    public void deleteById(Integer id) {
        clazzMapper.deleteById(id);
    }

    @Override
    public void add(Clazz clazz) {
        clazzMapper.insert(clazz);
    }

    @Override
    public Clazz getById(Integer id) {
        return clazzMapper.getById(id);
    }

    @Override
    public void update(Clazz clazz) {
        clazzMapper.updateById(clazz);
    }

    @Override
    public List<Clazz> listAll() {
        return clazzMapper.listAll();
    }
}
