package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpLog {
    private Integer id; //ID
    private Integer operateEmpId; //操作人ID
    private LocalDateTime operateTime; //操作时间
    private String className; //类名
    private String methodName; //方法名
    private String methodParams; //方法参数
    private String returnValue; //返回值
    private Long costTime; //耗时

    private String operateEmpName; //操作人姓名（关联查询）
}
