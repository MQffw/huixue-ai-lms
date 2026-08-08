package com.itheima.interceptor;

import com.itheima.config.JwtConfig;
import com.itheima.utils.CurrentHolder;
import com.itheima.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 令牌校验拦截器 - 支持自动续期和RBAC权限
 */
@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private JwtConfig jwtConfig;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        
        // 1. 放行公开路径
        if (isPublicPath(requestURI)) {
            log.info("公开路径放行: {}", requestURI);
            return true;
        }
        
        // 2. 获取Token
        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) {
            log.warn("令牌不存在: {}", requestURI);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录\"}");
            return false;
        }
        
        // 3. 校验Token
        try {
            Claims claims = jwtUtils.parseToken(token);
            
            // 4. 检查是否需要续期
            if (jwtUtils.shouldRenew(claims)) {
                Map<String, Object> newClaims = new HashMap<>();
                newClaims.put("id", claims.get("id"));
                newClaims.put("username", claims.get("username"));
                newClaims.put("role", claims.get("role"));
                String newToken = jwtUtils.generateToken(newClaims);
                response.setHeader("new-token", newToken);
                log.info("Token已续期: {}", claims.get("id"));
            }
            
            // 5. 设置当前用户上下文
            Integer empId = claims.get("id", Integer.class);
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);
            
            CurrentHolder.setId(empId);
            CurrentHolder.setUsername(username);
            CurrentHolder.setRole(role);
            
            log.info("用户认证成功: id={}, role={}, uri={}", empId, role, requestURI);
            
            return true;
            
        } catch (Exception e) {
            log.warn("令牌校验失败: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"令牌无效\"}");
            return false;
        }
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 强制清理ThreadLocal - 防止内存泄漏和身份串号
        try {
            Integer userId = CurrentHolder.getId();
            CurrentHolder.remove();
            log.debug("用户上下文已清理: userId={}", userId);
        } catch (Exception e) {
            log.error("清理用户上下文异常", e);
            // 强制清理
            CurrentHolder.remove();
        }
    }
    
    /**
     * 判断是否为公开路径
     */
    private boolean isPublicPath(String uri) {
        return uri.startsWith("/login")
            || uri.startsWith("/public/")
            || uri.equals("/health")
            || uri.startsWith("/actuator")
            || uri.startsWith("/swagger")
            || uri.startsWith("/v3/api-docs")
            || uri.startsWith("/doc.html")
            || uri.startsWith("/webjars")
            || uri.startsWith("/favicon");
    }
}
