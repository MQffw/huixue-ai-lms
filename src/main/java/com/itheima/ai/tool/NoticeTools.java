package com.itheima.ai.tool;

import com.itheima.mapper.NoticeMapper;
import com.itheima.pojo.Notice;
import com.itheima.security.sql.SecureSqlExecutor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 通知/制度/公告域 Tool（3 个业务 + 1 个兜底 SQL 工具 = 4 个）
 */
@Component
public class NoticeTools {

    @Autowired
    private NoticeMapper noticeMapper;

    @Autowired
    private SecureSqlExecutor secureSqlExecutor;

    /**
     * 关键词搜索通知公告
     */
    @Tool(description = """
            按关键词搜索通知/公告/制度类文档。
            参数：keyword（字符串，至少 1 字符）
            返回：每行 "【类型】标题 [置顶]\n摘要(前100字)..."；无结果返回 "未找到与「X」相关的通知公告"
            注意：适合搜索制度、流程、规定类文档，不适合查询具体业务数据。
            """)
    public String searchNotice(String keyword) {
        List<Notice> list = noticeMapper.searchByKeyword(keyword);
        if (list.isEmpty()) return "未找到与「" + keyword + "」相关的通知公告";
        StringBuilder sb = new StringBuilder("搜索「" + keyword + "」找到" + list.size() + "条结果：\n");
        for (Notice n : list) {
            String typeStr = n.getType() != null && n.getType() == 1 ? "通知" : n.getType() != null && n.getType() == 2 ? "公告" : "制度";
            sb.append("【").append(typeStr).append("】").append(n.getTitle());
            if (n.getIsTop() != null && n.getIsTop() == 1) sb.append(" [置顶]");
            sb.append("\n");
            String content = n.getContent();
            if (content != null && content.length() > 100) {
                content = content.substring(0, 100) + "...";
            }
            sb.append(content).append("\n---\n");
        }
        return sb.toString();
    }

    /**
     * 查最新通知公告
     */
    @Tool(description = """
            查询最新的通知公告列表。
            参数：limit（整数，可选，默认5，最大20）
            返回：每行 "ID:N 【类型】标题 [置顶]（yyyy-MM-dd）"
            """)
    public String getLatestNotices(Integer limit) {
        if (limit == null || limit <= 0) limit = 5;
        if (limit > 20) limit = 20;
        List<Notice> list = noticeMapper.findLatest(limit);
        if (list.isEmpty()) return "暂无通知公告";
        StringBuilder sb = new StringBuilder("最新通知公告（" + list.size() + "条）：\n");
        for (Notice n : list) {
            String typeStr = n.getType() != null && n.getType() == 1 ? "通知" : n.getType() != null && n.getType() == 2 ? "公告" : "制度";
            sb.append("ID:").append(n.getId()).append(" ");
            sb.append("【").append(typeStr).append("】").append(n.getTitle());
            if (n.getIsTop() != null && n.getIsTop() == 1) sb.append(" [置顶]");
            sb.append("（").append(n.getPublishTime() != null ? n.getPublishTime().toString().substring(0, 10) : "未知").append("）\n");
        }
        return sb.toString();
    }

    /**
     * 按 ID 查通知详情
     */
    @Tool(description = """
            按通知ID查询详情。
            参数：id（整数，必填）
            返回：完整标题/类型/发布时间/发布者/正文
            """)
    public String getNoticeById(Integer id) {
        Notice n = noticeMapper.getById(id);
        if (n == null) return "未找到ID为" + id + "的通知公告";
        String typeStr = n.getType() != null && n.getType() == 1 ? "通知" : n.getType() != null && n.getType() == 2 ? "公告" : "制度";
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(typeStr).append("】").append(n.getTitle()).append("\n");
        if (n.getIsTop() != null && n.getIsTop() == 1) sb.append("[置顶] ");
        sb.append("发布时间：").append(n.getPublishTime() != null ? n.getPublishTime().toString().substring(0, 10) : "未知");
        sb.append(" 发布者：").append(n.getPublisherName() != null ? n.getPublisherName() : "未知").append("\n\n");
        String content = n.getContent();
        sb.append(content != null ? content : "（无正文内容）");
        return sb.toString();
    }

    /**
     * SQL 兜底查询工具 — 已禁用（不再暴露给 AI）
     * 原因：SecureSqlExecutor 已 @Deprecated，且此工具可查询审计表（token_usage/tool_call_log），不应暴露给 LLM
     */
    // @Tool 已移除，AI 不再能执行任意 SQL
    public String executeQuery(String sql) {
        return "[安全限制] 此工具已禁用，请使用具体业务工具查询";
    }
}
