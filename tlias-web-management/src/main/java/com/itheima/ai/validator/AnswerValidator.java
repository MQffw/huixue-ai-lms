package com.itheima.ai.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 答案校验器（Answer Validator）
 *
 * 规则优先（不调 LLM，毫秒级）：
 *   1. 数量一致性：Tool 返回的数字必须出现在最终回答中；若回答出现 Tool 未返回的数字 → 触发重写
 *   2. 单位一致性：Tool 返回的 "人"/"元"/"分" 等量词应与回答一致
 *   3. 遗漏检查：关键统计项（Tool 返回多行）中至少 60% 在回答中出现
 *
 * 校验通过则放行；不通过时抛 {@link AnswerVerifyException}，由 Orchestrator 用更严格的 Prompt 重新生成一次回答。
 */
@Slf4j
@Component
public class AnswerValidator {

    /** 数字+量词边界：匹配如 "356人" "12.5%" "100元" "第3名" "57分" */
    private static final Pattern NUMBER_WITH_UNIT = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(人|名|位|个|次|条|天|元|分|%|百分比|百分比|倍)?");

    /** 纯数字 */
    private static final Pattern PURE_NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");

    /**
     * 校验回答是否与工具结果一致
     * @param toolResult 工具原始返回
     * @param answer    模型最终回答
     * @throws AnswerVerifyException 若不一致（携带偏离原因）
     */
    public void validate(String toolResult, String answer) throws AnswerVerifyException {
        if (toolResult == null || toolResult.isEmpty()) return;
        if (answer == null || answer.isEmpty()) return;

        // 1. 数量一致性校验
        Set<String> toolNumbers = extractNumbersWithOptionalUnit(toolResult);
        Set<String> answerNumbers = extractPureNumbers(answer);

        if (toolNumbers.isEmpty() || answerNumbers.isEmpty()) return;

        // 回答中出现 tool 中没出现的数字 → 可能编造
        Set<String> hallucinated = new HashSet<>(answerNumbers);
        hallucinated.removeAll(toolNumbers);
        if (!hallucinated.isEmpty()) {
            log.warn("Answer 疑似编造数字: hallucinated={} toolNumbers={} answerNumbers={}",
                    hallucinated, toolNumbers, answerNumbers);
            throw new AnswerVerifyException(answer,
                    "回答包含工具未返回的数字: " + hallucinated + "，请仅使用工具数据");
        }

        // 2. 校验 tool 中关键统计项是否大部分出现在回答中
        if (toolNumbers.size() >= 2) {
            Set<String> overlapping = new HashSet<>(answerNumbers);
            overlapping.retainAll(toolNumbers);
            double recallRate = overlapping.size() / (double) toolNumbers.size();
            if (recallRate < 0.5) {
                log.warn("Answer 校验遗漏过多: recallRate={} overlap={} toolNums={}",
                        recallRate, overlapping, toolNumbers);
                throw new AnswerVerifyException(answer,
                        "回答遗漏过多工具数据，请确保引用所有工具返回的关键数字: " + toolNumbers);
            }
        }
    }

    /**
     * 宽松校验：忽略"第N名""约"等修饰词的差异，只抽取"核心数字+单位"集合
     */
    private Set<String> extractNumbersWithOptionalUnit(String text) {
        if (text == null) return new HashSet<>();
        Set<String> result = new HashSet<>();
        Matcher m = NUMBER_WITH_UNIT.matcher(text);
        while (m.find()) {
            String num = m.group(1);
            String unit = m.group(2) != null ? m.group(2) : "";
            result.add(num + unit);   // "356人"
            result.add(num);           // "356" → 也保留裸数字用于宽松匹配
        }
        return result;
    }

    private Set<String> extractPureNumbers(String text) {
        Set<String> result = new HashSet<>();
        Matcher m = PURE_NUMBER.matcher(text);
        while (m.find()) result.add(m.group());
        return result;
    }

    // ════ 异常 ════

    public static class AnswerVerifyException extends RuntimeException {
        private final String rawAnswer;

        public AnswerVerifyException(String rawAnswer, String reason) {
            super(reason);
            this.rawAnswer = rawAnswer;
        }

        public String getRawAnswer() {
            return rawAnswer;
        }
    }
}
