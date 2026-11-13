package com.huixuetong.mapper;

import com.huixuetong.pojo.Clazz;
import com.huixuetong.pojo.ClazzQueryParam;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ClazzMapper {
    List<Clazz> pageList(ClazzQueryParam param);

    Long count(ClazzQueryParam param);

    void deleteById(Integer id);

    void deleteByIds(@org.apache.ibatis.annotations.Param("ids") List<Integer> ids);

    void insert(Clazz clazz);

    Clazz getById(Integer id);

    void updateById(Clazz clazz);

    List<Clazz> listAll();
}
