package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 缴费记录实体
 * 对应表：payment
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    private Integer id;
    private Integer studentId;
    private BigDecimal amount;     // 缴费金额
    private String paymentType;    // 费用类型: 学费/住宿费/教材费/押金
    private String paymentMethod;  // 缴费方式: 现金/微信/支付宝/银行转账
    private LocalDate paymentDate;
    private Integer status;        // 1:已缴费 2:待确认 3:已退款
    private Integer operatorId;
    private String remark;
    private LocalDateTime createTime;

    // 关联查询字段
    private String studentName;
    private String clazzName;
}
