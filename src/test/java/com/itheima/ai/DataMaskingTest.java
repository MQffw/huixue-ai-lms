package com.itheima.ai;

import com.itheima.ai.common.DataMasker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据脱敏测试（使用独立的 DataMasker）
 */
@DisplayName("数据脱敏")
class DataMaskingTest {

    @ParameterizedTest
    @CsvSource({
        "13812345678, 138****5678",
        "15900001111, 159****1111",
        "18888888888, 188****8888",
    })
    @DisplayName("手机号脱敏 → 中间4位变****")
    void shouldMaskPhone(String input, String expected) {
        String result = DataMasker.mask("手机号：" + input);
        assertTrue(result.contains(expected));
    }

    @ParameterizedTest
    @CsvSource({
        "110101199001011234, 110101********34",
        "32010219850615789X, 320102********9X",
    })
    @DisplayName("身份证脱敏 → 中间变********")
    void shouldMaskIdCard(String input, String expected) {
        String result = DataMasker.mask("身份证：" + input);
        assertTrue(result.contains(expected), "Expected result to contain: " + expected + " but got: " + result);
    }

    @Test
    @DisplayName("同一文本中多个敏感信息 → 全部脱敏")
    void shouldMaskMultipleSensitiveData() {
        String text = "学员张三，手机13800001111，身份证110101199001011234";
        String result = DataMasker.mask(text);
        assertTrue(result.contains("138****1111"));
        assertTrue(result.contains("110101********34"));
    }

    @Test
    @DisplayName("无敏感信息 → 原样返回")
    void shouldReturnOriginalText() {
        String text = "Java班共有20名学员";
        String result = DataMasker.mask(text);
        assertEquals(text, result);
    }

    @Test
    @DisplayName("null返回null")
    void shouldHandleNull() {
        assertNull(DataMasker.mask(null));
    }

    @Test
    @DisplayName("空字符串 → 返回空")
    void shouldHandleEmpty() {
        assertEquals("", DataMasker.mask(""));
    }
}
