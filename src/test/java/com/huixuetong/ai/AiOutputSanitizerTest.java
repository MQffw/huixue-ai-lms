package com.huixuetong.ai;

import com.huixuetong.ai.common.AiOutputSanitizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模型输出清洗器测试。
 * 用真实泄漏样本做回归：DSML（DeepSeek）、LongCat、旧版 [[TOOL]]。
 */
class AiOutputSanitizerTest {

    @Test
    @DisplayName("DSML 调用块整体泄漏时应被完全清除")
    void removesDsmlBlock() {
        String raw = """
                <｜｜DSML｜｜ calls>
                <｜｜DSML｜｜ invoke name="searchNotice">
                <｜｜DSML｜｜ parameter name="keyword" string="true">缓考</｜｜DSML｜｜ parameter>
                </｜｜DSML｜｜ invoke>
                </｜｜DSML｜｜ calls>""";

        assertTrue(AiOutputSanitizer.containsToolMarkup(raw));
        assertEquals("", AiOutputSanitizer.sanitize(raw));
    }

    @Test
    @DisplayName("DSML 块后面跟正常回答时，只删标记、保留回答")
    void keepsProseAfterDsmlBlock() {
        String raw = """
                <｜｜DSML｜｜ calls>
                <｜｜DSML｜｜ invoke name="searchNotice">
                <｜｜DSML｜｜ parameter name="keyword" string="true">缓考</｜｜DSML｜｜ parameter>
                </｜｜DSML｜｜ invoke>
                </｜｜DSML｜｜ calls>

                缓考需要提交申请表，由班主任审批。""";

        assertEquals("缓考需要提交申请表，由班主任审批。", AiOutputSanitizer.sanitize(raw));
    }

    @Test
    @DisplayName("ASCII 竖线写法的 DSML 也应识别")
    void removesAsciiDsml() {
        String raw = "<||DSML|| invoke name=\"x\"><||DSML|| parameter name=\"k\">1</||DSML|| parameter>";
        assertTrue(AiOutputSanitizer.containsToolMarkup(raw));
        assertEquals("", AiOutputSanitizer.sanitize(raw));
    }

    @Test
    @DisplayName("LongCat 工具调用块应被清除")
    void removesLongCatBlock() {
        String raw = """
                <longcat_tool_call>
                {"name":"searchNotice","arguments":{"keyword":"缓考"}}
                </longcat_tool_call>""";
        assertTrue(AiOutputSanitizer.containsToolMarkup(raw));
        assertEquals("", AiOutputSanitizer.sanitize(raw));
    }

    @Test
    @DisplayName("旧版 [[TOOL]] 标记应被清除")
    void removesLegacyToolBlock() {
        String raw = "[[TOOL]]searchNotice{\"keyword\":\"缓考\"}[[END_TOOL]]";
        assertTrue(AiOutputSanitizer.containsToolMarkup(raw));
        assertEquals("", AiOutputSanitizer.sanitize(raw));
    }

    @Test
    @DisplayName("正常回答不应被改动")
    void keepsPlainAnswer() {
        String raw = "目前共有 18 名学员，其中本科学历 8 人、大专 6 人、高中 4 人。";
        assertFalse(AiOutputSanitizer.containsToolMarkup(raw));
        assertEquals(raw, AiOutputSanitizer.sanitize(raw));
    }

    @Test
    @DisplayName("null 与空串安全返回空串")
    void handlesNullAndEmpty() {
        assertEquals("", AiOutputSanitizer.sanitize(null));
        assertEquals("", AiOutputSanitizer.sanitize(""));
        assertFalse(AiOutputSanitizer.containsToolMarkup(null));
    }

    @Test
    @DisplayName("清洗后只剩标点的输出视为空")
    void punctuationOnlyBecomesEmpty() {
        assertEquals("", AiOutputSanitizer.sanitize("。。。"));
    }
}
