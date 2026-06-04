package com.itheima.pojo;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ClazzQueryParam {
    private Integer page = 1;
    private Integer pageSize = 10;
    private Integer start;       // 分页起始位置
    private String name;        // 班级名称
    private LocalDate begin;    // 结课开始时间
    private LocalDate end;      // 结课结束时间
}