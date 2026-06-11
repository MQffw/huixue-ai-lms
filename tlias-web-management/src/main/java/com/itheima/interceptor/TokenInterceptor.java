package com.itheima.interceptor;

import com.itheima.utils.CurrentHolder;
import com.itheima.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 令牌校验拦截器 - 支持自动续期
 */
@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {

    // token续期阈值：距离过期时间小于30分钟就续期
    private static final long RENEWAL_THRESHOLD = 30 * 60 * 1000;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.获取请求路径
        String requestURI = request.getRequestURI();

        //2.判断是否是登录操作
        if (requestURI.startsWith("/login")) {
            log.info("登录操作,放行");
            return true;
        }

        //3.获取请求头中的令牌
        String token = request.getHeader("token");

        //4.判断token是否存在
        if (token == null || token.isEmpty()) {
            log.info("令牌不存在,拦截");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        //5.校验令牌
        try {
            Claims claims = JwtUtils.parseToken(token);

            // 检查是否需要续期
            Date expiration = claims.getExpiration();
            long timeUntilExpiry = expiration.getTime() - System.currentTimeMillis();

            // 如果token将在30分钟内过期，则生成新token
            if (timeUntilExpiry < RENEWAL_THRESHOLD && timeUntilExpiry > 0) {
                // 生成新token
                Map<String, Object> newClaims = new HashMap<>();
                newClaims.put("id", claims.get("id"));
                newClaims.put("username", claims.get("username"));
                String newToken = JwtUtils.generateToken(newClaims);

                // 在响应头中返回新token
                response.setHeader("new-token", newToken);
                log.info("Token已续期,用户ID: {}", claims.get("id"));
            }

            //将当前登录用户ID存入ThreadLocal
            Integer empId = (Integer) claims.get("id");
            CurrentHolder.setId(empId);

        } catch (Exception e) {
            log.info("令牌校验失败,拦截");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        //6.令牌校验成功，放行
        log.info("令牌校验成功,放行");
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //请求结束，清除ThreadLocal
        CurrentHolder.remove();
    }
}
