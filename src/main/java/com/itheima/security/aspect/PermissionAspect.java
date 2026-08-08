package com.itheima.security.aspect;

import com.itheima.pojo.Result;
import com.itheima.security.annotation.RequiresRole;
import com.itheima.utils.CurrentHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 权限切面
 * 当前系统仅管理员角色，校验用户是否已登录即可
 */
@Aspect
@Component
@Slf4j
public class PermissionAspect {

    @Around("@annotation(requiresRole)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequiresRole requiresRole) throws Throwable {
        if (CurrentHolder.getId() == null) {
            log.warn("未登录，拒绝访问");
            return Result.error("未登录");
        }
        return joinPoint.proceed();
    }
}
