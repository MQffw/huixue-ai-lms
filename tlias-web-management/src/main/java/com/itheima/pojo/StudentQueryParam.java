package com.itheima.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

/**
 * 学员查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StudentQueryParam extends PageQueryParam {

    private String name;
    private LocalDate beginDate;
    private LocalDate endDate;
    private Integer clazzId;
    private Integer degree;
}
