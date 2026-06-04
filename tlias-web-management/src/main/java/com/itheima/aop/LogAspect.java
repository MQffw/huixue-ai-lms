package com.itheima.aop;

import com.itheima.pojo.EmpLog;
import com.itheima.service.EmpLogService;
import com.itheima.utils.CurrentHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LogAspect {

    @Autowired
    private EmpLogService empLogService;

    //拦截controller包下所有类的Post/PUT/DeleteMapping方法，排除登录
    @Pointcut("execution(* com.itheima.controller.*.*(..)) && " +
              "!execution(* com.itheima.controller.LoginController.*(..)) && " +
              "(@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
              " @annotation(org.springframework.web.bind.annotation.PutMapping) || " +
              " @annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public void controllerWriteMethods() {}

    @Around("controllerWriteMethods()")
    public Object recordLog(ProceedingJoinPoint joinPoint) throws Throwable {
        //操作开始时间
        long startTime = System.currentTimeMillis();

        Object result = null;
        try {
            //执行目标方法
            result = joinPoint.proceed();
            return result;
        } finally {
            //操作耗时
            long costTime = System.currentTimeMillis() - startTime;

            //构建日志对象
            EmpLog empLog = new EmpLog();
            empLog.setOperateEmpId(CurrentHolder.getId());
            empLog.setOperateTime(LocalDateTime.now());
            empLog.setClassName(joinPoint.getTarget().getClass().getName());
            empLog.setMethodName(joinPoint.getSignature().getName());
            //方法参数（截取前500字符，避免过长）
            String params = Arrays.toString(joinPoint.getArgs());
            if (params.length() > 500) {
                params = params.substring(0, 500) + "...";
            }
            empLog.setMethodParams(params);
            //返回值（截取前500字符）
            if (result != null) {
                String ret = result.toString();
                if (ret.length() > 500) {
                    ret = ret.substring(0, 500) + "...";
                }
                empLog.setReturnValue(ret);
            }
            empLog.setCostTime(costTime);

            //异步记录日志，不影响主业务性能
            try {
                empLogService.insertLog(empLog);
            } catch (Exception e) {
                log.error("记录操作日志失败", e);
            }

            log.info("操作日志已记录: {}.{} 耗时:{}ms",
                    joinPoint.getTarget().getClass().getSimpleName(),
                    joinPoint.getSignature().getName(),
                    costTime);
        }
    }
}
