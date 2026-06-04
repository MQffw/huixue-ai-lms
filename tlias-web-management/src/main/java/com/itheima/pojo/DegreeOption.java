package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学员学历统计选项
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DegreeOption {
    private String name;  // 学历名称
    private Integer value; // 人数
}