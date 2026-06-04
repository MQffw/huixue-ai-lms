package com.itheima.interceptor;

import com.itheima.utils.CurrentHolder;
import com.itheima.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 令牌校验拦截器
 */
@Slf4j
@Component//交给springioc容器管理
public class TokenInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.获取请求路径
        String requertURI = request.getRequestURI();

        //2.判断是否是登录操作
        if (requertURI.startsWith("/login")){
            log.info("登录操作,放行");
            return true;
        }

        //3.获取请求头中的令牌
        String token = request.getHeader("token");

        //4.判断token是否存在
        if (token == null || token.isEmpty()){
            log.info("令牌不存在,拦截");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        //5.如果token存在，校验令牌，校验失败返回401
        try {
            Claims claims = JwtUtils.parseToken(token);
            //将当前登录用户ID存入ThreadLocal
            Integer empId = (Integer) claims.get("id");
            CurrentHolder.setId(empId);
        } catch (Exception e) {
            log.info("令牌校验失败,拦截");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        //6.如果令牌校验成功，放行
        log.info("令牌校验成功,放行");
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //请求结束，清除ThreadLocal
        CurrentHolder.remove();
    }
}
