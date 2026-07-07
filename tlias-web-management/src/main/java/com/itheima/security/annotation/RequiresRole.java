package com.itheima.security.annotation;

import java.lang.annotation.*;

/**
 * 角色权限注解 - 用于Controller方法或类
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRole {
    String[] value();  // 允许的角色列表
    String description() default "";
}
