package com.itheima.interceptor;

import com.google.common.util.concurrent.RateLimiter;
import com.itheima.pojo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 限流拦截器 - 防止恶意高频调用
 */
@Slf4j
@Component
@Order(1) // 优先级最高，在TokenInterceptor之前执行
public class RateLimitInterceptor implements HandlerInterceptor {
    
    @Autowired
    private RateLimiter normalRateLimiter;
    
    @Autowired
    private RateLimiter aiRateLimiter;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        
        // 选择限流器
        RateLimiter limiter = isAiEndpoint(uri) ? aiRateLimiter : normalRateLimiter;
        String limiterType = isAiEndpoint(uri) ? "AI" : "normal";
        
        // 尝试获取令牌（非阻塞，立即返回）
        if (limiter.tryAcquire()) {
            return true;
        }
        
        // 限流 - 返回429状态码
        log.warn("接口限流: uri={}, limiter={}, rate={}", uri, limiterType, 
                limiterType.equals("AI") ? "10/s" : "100/s");
        
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":429,\"msg\":\"请求过于频繁，请稍后重试\"}");
        return false;
    }
    
    /**
     * 判断是否为AI接口
     */
    private boolean isAiEndpoint(String uri) {
        return uri != null && uri.startsWith("/ai/");
    }
}
