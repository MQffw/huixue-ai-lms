package com.huixuetong.ai;

import com.huixuetong.ai.rag.SentenceSplitter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分块器回归：保证标题不被压平、块大小有上限。
 */
class SentenceSplitterTest {

    @Test
    @DisplayName("保留换行：标题行不会被并入上一段")
    void keepsHeadings() {
        String text = "第1章 引言\n1.1 选题背景\n本课题来源于实际项目。\n1.2 研究意义\n具有重要意义。";
        List<String> chunks = SentenceSplitter.split(text, 500, 100);

        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).startsWith("第1章 引言\n"), "标题应在块首且保留换行");
        assertTrue(chunks.get(0).contains("\n1.1 选题背景\n"));
    }

    @Test
    @DisplayName("超长英文段落也要被切开，块大小不超过 chunkSize")
    void hardSplitLongParagraph() {
        String paragraph = "This is a long English paragraph without Chinese punctuation. ".repeat(40);
        List<String> chunks = SentenceSplitter.split(paragraph, 500, 100);

        assertTrue(chunks.size() > 1);
        for (String c : chunks) {
            assertTrue(c.length() <= 500, "块长度超限: " + c.length());
        }
    }

    @Test
    @DisplayName("多段长文本：每块不超过 chunkSize，且无空块")
    void chunkSizeBounded() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            sb.append("第").append(i).append("条 这是第").append(i).append("条规定的说明文字，用于测试分块边界与重叠。\n");
        }
        List<String> chunks = SentenceSplitter.split(sb.toString(), 500, 100);

        assertTrue(chunks.size() > 1);
        for (String c : chunks) {
            assertFalse(c.isBlank());
            assertTrue(c.length() <= 500, "块长度超限: " + c.length());
        }
    }

    @Test
    @DisplayName("空文本返回空列表")
    void emptyText() {
        assertTrue(SentenceSplitter.split("", 500, 100).isEmpty());
        assertTrue(SentenceSplitter.split(null, 500, 100).isEmpty());
    }
}
