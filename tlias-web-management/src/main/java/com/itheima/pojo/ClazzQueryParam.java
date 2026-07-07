package com.itheima.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

/**
 * 班级查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClazzQueryParam extends PageQueryParam {

    private String name;
    private LocalDate begin;
    private LocalDate end;
}
