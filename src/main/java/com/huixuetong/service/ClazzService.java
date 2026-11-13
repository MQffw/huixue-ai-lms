package com.huixuetong.service;

import com.huixuetong.pojo.Clazz;
import com.huixuetong.pojo.ClazzQueryParam;
import com.huixuetong.pojo.PageResult;
import com.huixuetong.pojo.Result;

import java.util.List;

public interface ClazzService {
    PageResult<Clazz> page(ClazzQueryParam param);

    void deleteById(Integer id);

    void deleteByIds(List<Integer> ids);

    void add(Clazz clazz);

    Clazz getById(Integer id);

    void update(Clazz clazz);

    List<Clazz> listAll();
}