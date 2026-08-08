package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 就业记录实体
 * 对应表：employment
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employment {
    private Integer id;
    private Integer studentId;
    private Integer clazzId;
    private String company;
    private String position;
    private Integer salary;        // 入职薪资（元/月）
    private String city;
    private LocalDate employmentDate;
    private Integer status;        // 1:在职 2:离职 3:试用期
    private LocalDateTime createTime;

    // 关联查询字段
    private String studentName;
    private String clazzName;
}
