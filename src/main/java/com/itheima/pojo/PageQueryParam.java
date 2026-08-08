package com.itheima.pojo;

import lombok.Data;

/**
 * 分页查询参数基类
 */
@Data
public class PageQueryParam {

    private Integer page = 1;
    private Integer pageSize = 10;

    /**
     * 获取分页起始位置（供 MyBatis SQL 使用）
     */
    public Integer getStart() {
        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        if (pageSize > 100) pageSize = 100;
        return (page - 1) * pageSize;
    }
}
