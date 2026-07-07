package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 违纪记录明细实体
 * 对应表：violation_log
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViolationLog {
    private Integer id;
    private Integer studentId;
    private String violationType;  // 违纪类型: 迟到/旷课/作弊/打架/其他
    private LocalDate violationDate;
    private Integer deductScore;   // 扣分
    private String description;
    private Integer handlerId;
    private LocalDateTime createTime;

    // 关联查询字段
    private String studentName;
    private String handlerName;
}
