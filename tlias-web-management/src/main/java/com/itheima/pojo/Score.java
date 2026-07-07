package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成绩实体
 * 对应表：score
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Score {
    private Integer id;
    private Integer examId;
    private Integer studentId;
    private BigDecimal score;      // 分数
    private Integer rank;          // 排名
    private String remark;
    private LocalDateTime createTime;

    // 关联查询字段
    private String studentName;
    private String examName;
}
