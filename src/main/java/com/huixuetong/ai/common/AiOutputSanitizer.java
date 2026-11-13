package com.huixuetong.ai.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模型输出清洗器
 *
 * 背景：不同模型（DeepSeek 走自家 DSML、LongCat 走 <longcat_tool_call>、旧版走 [[TOOL]]）
 * 在工具调用未被框架识别时，会把"工具调用标记"当作正文吐给用户。
 * 前端已有一堆正则做兜底，但 /ai/chat/sync、定时任务、副驾都不经过前端，
 * 所以清洗必须放在服务端，作为输出契约的一部分。
 *
 * 处理顺序（每种标记都一样）：
 *   1. 反复删除"已闭合"的调用块（循环处理嵌套）
 *   2. 若仍残留未闭合标记，且位于开头，则整段丢弃（说明后面全是半截参数）
 *   3. 清理零散标签
 *   4. 压缩多余空行、去首尾空白
 *   5. 清洗后若只剩标点，视为空
 */
public final class AiOutputSanitizer {

    /** DSML 标记：全角竖线 ｜｜ 与 ASCII || 两种写法都见过 */
    private static final String DSML = "(?:｜｜DSML｜｜|\\|\\|DSML\\|\\|)";

    /** 已闭合的 DSML 块：任意开标签 → 最近的一个已知闭标签 */
    private static final Pattern DSML_BLOCK = Pattern.compile(
            "(?s)<" + DSML + ".*?</" + DSML + "\\s*(?:calls|invoke|parameter|tool_call|function)\\s*>");

    private static final Pattern DSML_OPEN = Pattern.compile("<" + DSML);
    private static final Pattern DSML_TAG = Pattern.compile("</?" + DSML + "[^>]*>");

    private static final Pattern LONGCAT_BLOCK = Pattern.compile(
            "(?s)<longcat_tool_call\\b[^>]*>.*?</longcat_tool_call\\s*>");
    private static final Pattern LONGCAT_OPEN = Pattern.compile("<longcat_tool_call\\b");
    private static final Pattern LONGCAT_TAG = Pattern.compile("</?longcat_tool_call\\b[^>]*>");

    private static final Pattern TOOL_BLOCK = Pattern.compile(
            "(?s)\\[\\[TOOL\\]\\].*?\\[\\[END_TOOL\\]\\]");
    private static final Pattern TOOL_OPEN = Pattern.compile("\\[\\[TOOL\\]\\]");
    private static final Pattern TOOL_TAG = Pattern.compile("\\[\\[END_TOOL\\]\\]|\\[\\[TOOL\\]\\]");

    private static final Pattern MULTI_BLANK_LINES = Pattern.compile("\\n{3,}");
    private static final Pattern ONLY_PUNCT = Pattern.compile("^[\\s\\p{Punct}，。！？；：、（）【】《》“”‘’…—]+$");
    private static final Pattern BLANK_RUN = Pattern.compile(" {2,}");

    private AiOutputSanitizer() {
    }

    /**
     * 清洗模型输出中的工具调用标记。
     *
     * @param text 模型原始输出，可为 null
     * @return 清洗后的文本；若清洗后无有效内容返回空串（由调用方决定兜底话术）
     */
    public static String sanitize(String text) {
        if (text == null || text.isEmpty()) return "";

        String out = text;
        out = stripBlocks(out, DSML_BLOCK, DSML_OPEN, DSML_TAG);
        out = stripBlocks(out, LONGCAT_BLOCK, LONGCAT_OPEN, LONGCAT_TAG);
        out = stripBlocks(out, TOOL_BLOCK, TOOL_OPEN, TOOL_TAG);

        out = MULTI_BLANK_LINES.matcher(out).replaceAll("\n\n");
        out = BLANK_RUN.matcher(out).replaceAll(" ");
        out = out.trim();

        // 只剩标点/空白，等同于没有回答
        if (out.isEmpty() || ONLY_PUNCT.matcher(out).matches()) return "";
        return out;
    }

    /**
     * 是否含有工具调用标记（用于日志告警、缓存写入判定、评测统计）
     */
    public static boolean containsToolMarkup(String text) {
        if (text == null || text.isEmpty()) return false;
        return DSML_OPEN.matcher(text).find()
                || LONGCAT_OPEN.matcher(text).find()
                || TOOL_OPEN.matcher(text).find();
    }

    private static String stripBlocks(String text, Pattern block, Pattern open, Pattern tag) {
        String out = text;
        // 1. 已闭合的块：循环删除，处理嵌套
        for (int i = 0; i < 50; i++) {
            Matcher m = block.matcher(out);
            if (!m.find()) break;
            out = m.replaceFirst("");
        }
        // 2. 未闭合且位于开头：整段丢弃（后面是半截参数，保留无意义）
        Matcher openMatcher = open.matcher(out);
        if (openMatcher.find() && out.substring(0, openMatcher.start()).trim().isEmpty()) {
            out = out.substring(0, openMatcher.start());
        }
        // 3. 零散标签
        out = tag.matcher(out).replaceAll("");
        return out;
    }
}
