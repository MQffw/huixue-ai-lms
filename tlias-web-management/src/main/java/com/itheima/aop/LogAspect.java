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

    @Pointcut("execution(* com.itheima.controller.*.*(..)) && " +
              "!execution(* com.itheima.controller.LoginController.*(..)) && " +
              "(@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
              " @annotation(org.springframework.web.bind.annotation.PutMapping) || " +
              " @annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public void controllerWriteMethods() {}

    @Around("controllerWriteMethods()")
    public Object recordLog(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;

            try {
                EmpLog empLog = new EmpLog();
                empLog.setOperateEmpId(CurrentHolder.getId());
                empLog.setOperateTime(LocalDateTime.now());
                empLog.setClassName(joinPoint.getTarget().getClass().getName());
                empLog.setMethodName(joinPoint.getSignature().getName());
                
                String params = Arrays.toString(joinPoint.getArgs());
                if (params.length() > 500) {
                    params = params.substring(0, 500) + "...";
                }
                empLog.setMethodParams(params);
                
                if (result != null) {
                    String ret;
                    if (result instanceof org.springframework.web.servlet.mvc.method.annotation.SseEmitter) {
                        ret = "SSE流式响应";
                    } else {
                        ret = result.toString();
                        if (ret.length() > 500) {
                            ret = ret.substring(0, 500) + "...";
                        }
                    }
                    empLog.setReturnValue(ret);
                }
                empLog.setCostTime(costTime);

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
