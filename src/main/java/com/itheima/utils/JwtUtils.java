package com.itheima.utils;

import com.itheima.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类 - 支持密钥轮换
 */
@Component
public class JwtUtils {
    
    private final JwtConfig jwtConfig;
    private String currentSecret;
    private String previousSecret;
    
    public JwtUtils(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }
    
    @PostConstruct
    public void init() {
        this.currentSecret = jwtConfig.getSecret();
        // 优先读取环境变量，否则用硬编码旧密钥兜底（密钥轮换过渡期用，生产移除）
        String envPrev = System.getenv("JWT_PREVIOUS_SECRET");
        this.previousSecret = (envPrev != null && !envPrev.isEmpty()) ? envPrev : "aXRoZWltYQ==";
    }
    
    public String generateToken(Map<String, Object> claims) {
        return Jwts.builder()
                .signWith(SignatureAlgorithm.HS256, currentSecret)
                .addClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration()))
                .compact();
    }
    
    public Claims parseToken(String token) throws Exception {
        try {
            return parseWithSecret(token, currentSecret);
        } catch (Exception e) {
            if (previousSecret != null && !previousSecret.isEmpty()) {
                try {
                    return parseWithSecret(token, previousSecret);
                } catch (Exception ex) {
                    throw ex;
                }
            }
            throw e;
        }
    }
    
    private Claims parseWithSecret(String token, String secret) throws Exception {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }
    
    public boolean shouldRenew(Claims claims) {
        Date expiration = claims.getExpiration();
        long timeUntilExpiry = expiration.getTime() - System.currentTimeMillis();
        return timeUntilExpiry > 0 && timeUntilExpiry < jwtConfig.getRenewalThreshold();
    }
}
