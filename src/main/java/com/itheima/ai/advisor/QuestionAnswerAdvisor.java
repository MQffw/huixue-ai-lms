package com.itheima.ai.advisor;

import com.itheima.pojo.Notice;
import com.itheima.mapper.NoticeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RAG 问答增强器（M6 兼容版）
 * 从通知公告表中检索相关制度文档作为上下文注入
 * 完整 RAG 实现需要升级到 Spring AI GA + Redis Stack
 */
@Slf4j
@Component
public class QuestionAnswerAdvisor {

    @Autowired
    private NoticeMapper noticeMapper;

    /** 触发知识库检索的关键词 */
    private static final String[] TRIGGER_KEYWORDS = {
            "制度", "规定", "规章", "条例", "通知", "公告", "请假", "退费", "宿舍",
            "违纪", "处分", "就业", "推荐", "考核", "证书", "管理制", "流程"
    };

    /**
     * 从通知公告（制度类）检索与查询相关的内容
     * @param query 用户查询
     * @return 相关知识库上下文文本，无结果返回空字符串
     */
    public String retrieveContext(String query) {
        try {
            // 预检：问题是否跟制度/通知相关，不相关直接跳过
            if (!isRelevantToNotice(query)) return "";

            // 从制度类公告中搜索相关内容
            List<Notice> notices = noticeMapper.findByType(3); // type=3 制度
            if (notices.isEmpty()) return "";

            StringBuilder context = new StringBuilder();
            int count = 0;
            for (Notice notice : notices) {
                // 简单关键词匹配
                if (notice.getTitle() != null && containsKeyword(notice.getTitle(), query)
                        || notice.getContent() != null && containsKeyword(notice.getContent(), query)) {
                    context.append("【").append(notice.getTitle()).append("】\n");
                    String content = notice.getContent();
                    if (content != null) {
                        context.append(content.length() > 300 ? content.substring(0, 300) + "..." : content);
                    }
                    context.append("\n\n");
                    count++;
                    if (count >= 3) break; // 最多3条
                }
            }

            if (context.isEmpty()) return "";

            return "\n【知识库检索结果】\n以下是与用户问题相关的制度文档片段，请优先基于这些内容回答：\n\n" + context;
        } catch (Exception e) {
            log.debug("[QA-Advisor] 检索失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 判断问题是否与制度/通知相关
     */
    private boolean isRelevantToNotice(String query) {
        if (query == null || query.isEmpty()) return false;
        for (String keyword : TRIGGER_KEYWORDS) {
            if (query.contains(keyword)) return true;
        }
        return false;
    }

    private boolean containsKeyword(String text, String query) {
        if (text == null || query == null) return false;
        // 简单分词匹配
        for (String word : query.split("[\\s，。！？；：、]+")) {
            if (word.length() >= 2 && text.contains(word)) return true;
        }
        return false;
    }
}
