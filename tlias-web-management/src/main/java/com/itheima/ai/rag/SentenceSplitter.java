package com.itheima.ai.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * 简单句子分块器（SentenceSplitter）
 *
 * 参考 LangChain TokenTextSplitter 的 chunk + overlap 模型：
 *   - 按句子边界切（句号、问号、叹号、换行）
 *   - 当累积超过 chunkSize 时放到下一 chunk
 *   - overlap 字符的尾部内容会回带到下一个 chunk（避免上下文断裂）
 *
 * 注意：不依赖语义理解（分词、embedding 暂不使用）；纯字符级分块。
 */
public final class SentenceSplitter {

    private static final char[] SENTENCE_DELIMS = { '。', '？', '！', '\n', ';', '；' };

    private SentenceSplitter() {}

    /**
     * 分块
     * @param text 长文本
     * @param chunkSize 每块最大字符数（软限制，视句子边界）
     * @param overlap 与上一块的重叠字符数
     */
    public static List<String> split(String text, int chunkSize, int overlap) {
        if (text == null || text.isBlank()) return new ArrayList<>();
        text = text.replaceAll("\\s+", " ").trim();

        List<String> sentences = splitSentences(text);
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String sentence : sentences) {
            // 当前块 + 新句子超过块大小 → 写入现有块
            if (current.length() + sentence.length() > chunkSize && current.length() > 0) {
                chunks.add(current.toString().trim());
                // overlap 回带
                String tailTape = current.length() > overlap
                    ? current.substring(current.length() - overlap)
                    : current.toString();
                current = new StringBuilder(tailTape);
            }
            current.append(sentence);
        }
        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }

    /**
     * 按句子边界切分，保留分隔符在句子末尾
     */
    private static List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            current.append(c);
            if (isSentenceDelim(c)) {
                if (current.length() > 0) {
                    sentences.add(current.toString());
                    current = new StringBuilder();
                }
            }
        }
        if (current.length() > 0) {
            sentences.add(current.toString());
        }
        return sentences;
    }

    private static boolean isSentenceDelim(char c) {
        for (char d : SENTENCE_DELIMS) {
            if (c == d) return true;
        }
        return false;
    }
}
