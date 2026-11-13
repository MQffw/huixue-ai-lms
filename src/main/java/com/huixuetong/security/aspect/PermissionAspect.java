package com.huixuetong.security.aspect;

import com.huixuetong.pojo.Result;
import com.huixuetong.security.annotation.RequiresRole;
import com.huixuetong.utils.CurrentHolder;
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
