package com.itheima.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

/**
 * 员工查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EmpQueryParam extends PageQueryParam {

    private String name;
    private Integer gender;
    private LocalDate beginDate;
    private LocalDate endDate;
    private Integer deptId;
}
