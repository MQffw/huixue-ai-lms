package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 课程实体
 * 对应表：course
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {
    private Integer id;
    private String name;
    private Integer subject;       // 学科: 1:Java 2:前端 3:大数据 4:Python 5:Go 6:嵌入式
    private Integer hours;         // 课时数
    private String description;    // 课程简介
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
