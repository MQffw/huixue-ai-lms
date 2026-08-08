package com.itheima.ai.common;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 数据脱敏工具
 *
 * 手机号：13812345678 → 138****5678
 * 身份证：110101199001011234 → 110101********34
 *
 * 纯函数，可独立测试
 */
@Component
public class DataMasker {

    private static final Pattern PHONE_PATTERN    = Pattern.compile("(?<!\\d)(1[3-9]\\d)\\d{4}(\\d{4})(?!\\d)");
    private static final Pattern ID_CARD_PATTERN  = Pattern.compile("(?<!\\d)(\\d{6})\\d{8,10}(\\w{2,4})(?!\\d)");

    /**
     * 对输入文本执行手机号 + 身份证脱敏
     * @param text 原始文本
     * @return 脱敏后文本；输入 null/null 返回 null/原值
     */
    public static String mask(String text) {
        if (text == null) return null;
        if (text.isEmpty()) return "";
        text = PHONE_PATTERN.matcher(text).replaceAll("$1****$2");
        text = ID_CARD_PATTERN.matcher(text).replaceAll("$1********$2");
        return text;
    }
}
