package com.itheima.config.model;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 模型 API Key 加密器
 * - 支持三种存储形态：
 *   ${ENV_NAME}  不再支持：密钥统一由用户输入，保存时自动加密
 *   enc:base64   AES-GCM 加密存储（加密密钥来自环境变量 MODEL_KEY_SECRET）
 *   其他         明文，保存时自动加密为 enc: 格式
 * - 展示统一脱敏：${ENV} 原样；enc: 显示 enc:***；明文显示 前3***后3
 * - 日志永不输出 key
 */
@Slf4j
@Component
public class ModelKeyCipher {

    private static final String ENC_PREFIX = "enc:";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;
    private static final String DEV_SECRET = "dev-only-default-secret-2026!";

    @Value("${ai.key-secret:}")
    private String keySecret;

    @PostConstruct
    public void checkSecret() {
        if (!StringUtils.hasText(keySecret)) {
            log.warn("MODEL_KEY_SECRET 未配置，密钥加密将使用开发默认密钥（生产环境必须设置 ai.key-secret）");
        }
    }

    /** 加密明文 key；${ENV} 占位与 enc: 密文原样保留 */
    public String encrypt(String plainKey) {
        if (plainKey == null) return null;
        String k = plainKey.trim();
        if (k.isEmpty() || k.startsWith(ENC_PREFIX)) return k;
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(k.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return ENC_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("API Key 加密失败", e);
            throw new RuntimeException("API Key 加密失败", e);
        }
    }

    /** 解密 enc: 密文；其他形式原样返回 */
    public String decrypt(String stored) {
        if (stored == null || !stored.startsWith(ENC_PREFIX)) return stored;
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(ENC_PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(all, 0, IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(all, IV_LENGTH, all.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("API Key 解密失败（可能 MODEL_KEY_SECRET 已变更）", e);
            return "";
        }
    }

    /** 解析最终可用 key：enc: → 解密；其他（含明文）→ 原样返回 */
    public String resolve(String stored) {
        if (stored == null) return "";
        return decrypt(stored.trim());
    }

    /** 脱敏展示：enc: 密文显 enc:***；明文显 前3***后3 */
    public String mask(String stored) {
        if (stored == null) return "";
        String k = stored.trim();
        if (k.isEmpty()) return "";
        if (k.startsWith(ENC_PREFIX)) return "enc:***";
        if (k.length() <= 6) return "***";
        return k.substring(0, 3) + "***" + k.substring(k.length() - 3);
    }

    private javax.crypto.SecretKey secretKey() {
        String secret = StringUtils.hasText(keySecret) ? keySecret : DEV_SECRET;
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("密钥派生失败", e);
        }
    }
}