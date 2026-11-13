package com.huixuetong.ai.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * 结构友好的分块器（v2）
 *
 * v1 的问题：
 *   1) 把 \n 也压成空格，标题/段落边界丢失，大纲和正文粘成一条；
 *   2) 只认中文句号，"英文段落"整段没有分隔符 → 单块可达 1500+ 字；
 *   3) 没有硬上限。
 *
 * v2 策略：
 *   - 保留换行：每个自然行（标题/条款/段落）是一个独立单元；
 *   - 中英文标点都作为句子边界（英文句点仅在后面是空白/结尾时算）；
 *   - 超过 chunkSize 的单元先按标点切，仍超则硬切，保证块大小稳定；
 *   - 相邻块保留 overlap 字符，避免跨块语义断裂。
 */
public final class SentenceSplitter {

    private SentenceSplitter() {}

    public static List<String> split(String text, int chunkSize, int overlap) {
        if (text == null || text.isBlank()) return new ArrayList<>();

        List<String> units = splitUnits(normalize(text));
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String unit : units) {
            if (unit.isBlank()) continue;

            if (current.length() > 0 && current.length() + unit.length() > chunkSize) {
                chunks.add(current.toString().trim());
                current = new StringBuilder(tail(current.toString(), overlap));
            }

            if (unit.length() > chunkSize) {
                for (String piece : hardSplit(unit, chunkSize)) {
                    if (current.length() > 0 && current.length() + piece.length() > chunkSize) {
                        chunks.add(current.toString().trim());
                        current = new StringBuilder(tail(current.toString(), overlap));
                    }
                    current.append(piece);
                    if (current.length() >= chunkSize) {
                        chunks.add(current.toString().trim());
                        current = new StringBuilder(tail(current.toString(), overlap));
                    }
                }
            } else {
                current.append(unit);
            }
        }

        if (current.length() > 0) {
            String last = current.toString().trim();
            if (!last.isEmpty()) chunks.add(last);
        }
        return chunks;
    }

    /** 只压空格与制表符，保留换行（标题/段落边界） */
    private static String normalize(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f ]+", " ")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    /** 切成"句子/行"级单元，保留结尾分隔符 */
    private static List<String> splitUnits(String text) {
        List<String> units = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            current.append(c);

            boolean delimiter = c == '。' || c == '？' || c == '！'
                    || c == '；' || c == ';' || c == '\n'
                    || c == '?' || c == '!';
            // 英文句点：后面是空白或文本结尾才算，避免切坏小数/版本号
            if (c == '.' && (i + 1 == text.length() || Character.isWhitespace(text.charAt(i + 1)))) {
                delimiter = true;
            }

            if (delimiter) {
                units.add(current.toString());
                current.setLength(0);
            }
        }
        if (current.length() > 0) units.add(current.toString());
        return units;
    }

    /** 超长单元：优先在标点处切，找不到再硬切 */
    private static List<String> hardSplit(String unit, int size) {
        List<String> pieces = new ArrayList<>();
        int start = 0;
        while (start < unit.length()) {
            int end = Math.min(start + size, unit.length());
            if (end < unit.length()) {
                int cut = lastDelimiter(unit, start, end);
                if (cut > start + size / 2) end = cut;
            }
            pieces.add(unit.substring(start, end));
            start = end;
        }
        return pieces;
    }

    private static int lastDelimiter(String text, int from, int to) {
        for (int i = to - 1; i >= from; i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '；' || c == '，' || c == '；' || c == ',' || c == '.' || c == '\n') {
                return i + 1;
            }
        }
        return -1;
    }

    private static String tail(String text, int overlap) {
        if (overlap <= 0) return "";
        return text.length() > overlap ? text.substring(text.length() - overlap) : text;
    }
}
