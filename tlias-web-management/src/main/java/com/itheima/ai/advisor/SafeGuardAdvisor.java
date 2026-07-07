package com.itheima.ai.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Prompt Injection 安全拦截器（M6 兼容版）
 * 在 Controller 层调用前做正则匹配检测攻击模式
 */
@Slf4j
@Component
public class SafeGuardAdvisor {

    private static final Pattern[] ATTACK_PATTERNS = {
            Pattern.compile("(?i)ignore\\s+(all\\s+)?(previous|prior|above|before)\\s+(instructions?|prompts?|rules?)"),
            Pattern.compile("(?i)(reveal|show|display|print|output)\\s+(your|the)\\s+(system\\s+)?(prompt|instructions?|rules?)"),
            Pattern.compile("(?i)(pretend|act|roleplay)\\s+(you\\s+are|as\\s+(a|an))"),
            Pattern.compile("(?i)(you\\s+are\\s+now|from\\s+now\\s+on\\s+you\\s+are)"),
            Pattern.compile("(?i)(new\\s+instructions?|new\\s+rules?|override|overwrite)"),
            Pattern.compile("(?i)forget\\s+(everything|all|what)\\s+(you|i)\\s+(said|told|asked)"),
            Pattern.compile("(忽略|忘记|无视)(之前|所有)?(的)?(指令|提示|规则|要求|对话|系统)"),
            Pattern.compile("(显示|展示|输出|打印)(你的|系统|你)(系统)?(提示词|指令|规则)"),
            Pattern.compile("(现在|从现在开始)(你是|你扮演|你是角色|你是黑客|你是攻击者)"),
            Pattern.compile("(新的|重写|覆盖)(指令|规则|之前的)"),
            Pattern.compile("={3,}|-{3,}|_{3,}|#{3,}"),
            Pattern.compile("\\[SYSTEM\\]|\\[INST\\]|\\[PROMPT\\]"),
    };

    /**
     * 检查用户输入是否包含 Prompt Injection 攻击
     * @param userInput 用户输入文本
     * @throws PromptInjectionException 如果检测到攻击
     */
    public void check(String userInput) {
        if (userInput == null) return;
        for (Pattern pattern : ATTACK_PATTERNS) {
            if (pattern.matcher(userInput).find()) {
                log.warn("[SafeGuard] 检测到 Prompt Injection 攻击: pattern={}, input={}",
                        pattern.pattern(), userInput.substring(0, Math.min(userInput.length(), 200)));
                throw new PromptInjectionException("输入包含不安全的指令，已被安全拦截");
            }
        }
    }

    public static class PromptInjectionException extends RuntimeException {
        public PromptInjectionException(String message) {
            super(message);
        }
    }
}
