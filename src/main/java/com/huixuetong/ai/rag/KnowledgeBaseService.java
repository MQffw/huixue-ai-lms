package com.huixuetong.ai.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huixuetong.mapper.KnowledgeChunkMapper;
import com.huixuetong.mapper.KnowledgeDocMapper;
import com.huixuetong.mapper.NoticeMapper;
import com.huixuetong.pojo.KnowledgeChunk;
import com.huixuetong.pojo.KnowledgeDoc;
import com.huixuetong.pojo.Notice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 知识库服务（Phase 2 重构版）
 *
 * 真正的 RAG 链路：
 *   upload:   MultipartFile → 文本提取（Tika兜底UTF-8）→ 分句分块（SentenceSplitter 500字 + 100 overlap）
 *             → 存 knowledge_chunk → 更新 knowledge_doc 状态为 READY
 *   search:   用户 query → 关键词预处理 → MySQL 全文检索 (ngram) → Top20 → Re-rank 关键词密度 → Top3 拼接作为 LLM 上下文
 *
 * 过渡约束：Spring AI M6 无 GA 版 VectorStore，先以 MySQL FULLTEXT + 文本匹配兜底。
 *  后续升级到 Redis Stack 时只需替换 search 方法的检索层，其他接口不变。
 */
@Slf4j
@Service
public class KnowledgeBaseService {

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 100;
    private static final int TOP_K_RETRIEVE = 20;
    private static final int TOP_K_RERANK = 3;

    @Autowired private KnowledgeDocMapper knowledgeDocMapper;
    @Autowired private KnowledgeChunkMapper knowledgeChunkMapper;
    @Autowired private NoticeMapper noticeMapper;
    @Autowired private com.huixuetong.ai.cache.AiAnswerCache aiAnswerCache;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ════════════════════════════════════════════════════
    //  Upload：解析 + 切片 + 入库
    // ════════════════════════════════════════════════════

    /**
     * 解析 + 切片 + 入库
         * @return 文档 ID
     */
    public Long uploadDocument(MultipartFile file, String category) throws IOException {
        String originalName = normalizeFileName(file.getOriginalFilename());
        category = (category != null && !category.isEmpty()) ? category : "faq";

        // 1. 保存文件到本地
        Path uploadDir = Paths.get("uploads", "knowledge");
        Files.createDirectories(uploadDir);
        String safeName = UUID.randomUUID().toString().substring(0, 8) + "_" + originalName;
        Path targetPath = uploadDir.resolve(safeName);
        file.transferTo(targetPath);

        // 2. 占位文档（PROCESSING）
        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setFileName(originalName);
        doc.setCategory(category);
        doc.setChunkCount(0);
        doc.setStatus("PROCESSING");
        doc.setFilePath(targetPath.toString());
        knowledgeDocMapper.insert(doc);
        Long docId = doc.getId();

        // 3. 解析文本 + 分句 + 切片
        try {
            String text = extractText(file, targetPath);
            if (text == null || text.isBlank()) {
                throw new IOException("文档内容为空或无法解析");
            }
            List<String> chunks = new ArrayList<>();
            int bodyIdx = text.indexOf("【正文】");
            if (bodyIdx > 0) {
                // 目录/大纲单独成块（不再切分），保证检索一次能拿到完整章节结构
                String outline = text.substring(0, bodyIdx).trim();
                if (!outline.isBlank()) chunks.add(outline);
                String body = text.substring(bodyIdx + "【正文】".length());
                chunks.addAll(SentenceSplitter.split(body, CHUNK_SIZE, CHUNK_OVERLAP));
            } else {
                chunks.addAll(SentenceSplitter.split(text, CHUNK_SIZE, CHUNK_OVERLAP));
            }

            // 4. 逐条入库（分批提交）
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                String embeddingJson = "[]"; // 过渡期暂不向量化，直接空数组
                knowledgeChunkMapper.insert(docId, i, chunk, estimateTokens(chunk), embeddingJson);
            }

            // 5. 更新状态
            knowledgeDocMapper.updateStatus(docId, "READY", chunks.size());
            // 知识库变了，旧的"未收录"回答必须失效，否则 30 分钟内还会答旧结论
            aiAnswerCache.clear();
            log.info("知识库文档入库完成: docId={}, chunks={}, file={}", docId, chunks.size(), originalName);
            return docId;
        } catch (Exception e) {
            log.error("文档解析失败: {}", originalName, e);
            knowledgeDocMapper.updateStatus(docId, "FAILED", 0);
            throw new IOException("文档解析或入库失败: " + e.getMessage(), e);
        }
    }

    /** 修复 multipart 文件名被按 ISO-8859-1 解码后的乱码（浏览器/脚本都可能触发） */
    private String normalizeFileName(String name) {
        if (name == null || name.isEmpty()) return name;
        boolean hasCjk = name.codePoints().anyMatch(c -> c >= 0x4E00 && c <= 0x9FFF);
        if (hasCjk) return name;
        try {
            String fixed = new String(name.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            boolean fixedCjk = fixed.codePoints().anyMatch(c -> c >= 0x4E00 && c <= 0x9FFF);
            if (fixedCjk && !fixed.contains("\uFFFD")) return fixed;
        } catch (Exception ignored) {
            // 保持原样
        }
        return name;
    }

    /**
     * 文本提取：txt/md 直读，docx 用 POI，pdf 用 PDFBox。
     * （旧实现把非文本文件按 UTF-8 读字节，PDF/Word 全是乱码）
     */
    private String extractText(MultipartFile file, Path savedPath) throws IOException {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();

        try {
            if (name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".csv") || name.endsWith(".log")) {
                return normalize(Files.readString(savedPath, StandardCharsets.UTF_8));
            }
            if (name.endsWith(".docx")) {
                return normalize(extractDocx(savedPath));
            }
            if (name.endsWith(".pdf")) {
                return normalize(extractPdf(savedPath));
            }
        } catch (Exception e) {
            throw new IOException("文档解析失败（" + file.getOriginalFilename() + "）: " + e.getMessage(), e);
        }

        throw new IOException("暂不支持的文件类型：" + file.getOriginalFilename()
                + "，请上传 .txt / .md / .docx / .pdf");
    }

    private String extractDocx(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (var in = Files.newInputStream(path);
             var doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(in)) {

            // Word 的自动目录是域代码，解析不出文本；用标题样式生成一份大纲，作为可检索的"目录"
            String outline = buildOutline(doc);
            if (!outline.isBlank()) {
                sb.append("【文档目录/大纲】\n").append(outline).append("\n【正文】\n");
            }

            for (var p : doc.getParagraphs()) {
                if (p.getText() != null && !p.getText().isBlank()) {
                    sb.append(p.getText()).append('\n');
                }
            }
            for (var table : doc.getTables()) {
                for (var row : table.getRows()) {
                    for (var cell : row.getTableCells()) {
                        sb.append(cell.getText()).append('\t');
                    }
                    sb.append('\n');
                }
            }
        }
        return sb.toString();
    }

    /** 按标题样式（Heading 1-6 / 标题 1-6）生成文档大纲 */
    private String buildOutline(org.apache.poi.xwpf.usermodel.XWPFDocument doc) {
        StringBuilder sb = new StringBuilder();
        for (var p : doc.getParagraphs()) {
            String text = p.getText();
            if (text == null || text.isBlank()) continue;

            String styleId = p.getStyleID();
            String styleName = "";
            try {
                if (styleId != null && doc.getStyles() != null) {
                    var style = doc.getStyles().getStyle(styleId);
                    if (style != null && style.getName() != null) {
                        styleName = style.getName().toLowerCase();
                    }
                }
            } catch (Exception ignored) {
                // 样式表异常不影响正文提取
            }

            boolean heading = styleName.contains("heading") || styleName.contains("标题")
                    || (styleId != null && styleId.toLowerCase().matches("heading\\s*[1-6]"));
            if (heading) {
                sb.append("- ").append(text.trim()).append('\n');
            }
        }
        return sb.toString();
    }

    private String extractPdf(Path path) throws IOException {
        try (var doc = org.apache.pdfbox.Loader.loadPDF(path.toFile())) {
            return new org.apache.pdfbox.text.PDFTextStripper().getText(doc);
        }
    }

    /** 统一换行、去多余空白 */
    private String normalize(String text) {
        if (text == null) return "";
        return text.replace("\r\n", "\n").replace('\r', '\n')
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    // ════════════════════════════════════════════════════
    //  Search：全文检索 + Re-rank
    // ════════════════════════════════════════════════════

    /**
     * 带重排的搜索：Top20 → Re-rank 关键词密度 → Top3 拼接文本
     * @return 已排序的 chunk 列表（最多 TOP_K_RERANK 条）
     */
    public List<KnowledgeChunk> search(String query, int topK) {
        if (query == null || query.trim().isEmpty()) return Collections.emptyList();
        List<String> keywords = tokenize(query);
        if (keywords.isEmpty()) return Collections.emptyList();

        // 1. MySQL 全文检索兜底 LIKE（Top20）
        List<KnowledgeChunk> candidates;
        try {
            String queryLike = String.join(" ", keywords);
            candidates = knowledgeChunkMapper.searchFullText(query, queryLike, TOP_K_RETRIEVE);
        } catch (Exception e) {
            log.warn("全文索引失败，降级纯关键词搜索: {}", e.getMessage());
            candidates = fallbackLikeSearch(keywords);
        }

        // 2. 把 Notice 表也并入候选（制度类文档来自 Notice）
        List<KnowledgeChunk> noticeChunks = searchNoticeChunks(query, keywords);
        candidates.addAll(noticeChunks);

        if (candidates.isEmpty()) return Collections.emptyList();

        // 3. Re-rank：关键词命中密度 + 句长加权
        for (KnowledgeChunk chunk : candidates) {
            double score = rerankScore(chunk.getContent(), keywords);
            chunk.setSimilarityScore(score);
        }
        candidates.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));

        // 全部零分说明没有任何关键词命中，不要硬凑来源（交给上层拒答）
        if (candidates.get(0).getSimilarityScore() <= 0) {
            log.info("知识库检索无有效命中: query={}", query);
            return Collections.emptyList();
        }

        // 问"目录/大纲/章节"时，把带大纲的切片置顶（稳定排序，组内仍按得分）
        if (query.contains("目录") || query.contains("大纲") || query.contains("章节")) {
            candidates.sort((a, b) -> Boolean.compare(
                    b.getContent() != null && b.getContent().startsWith("【文档目录/大纲】"),
                    a.getContent() != null && a.getContent().startsWith("【文档目录/大纲】")));
        }

        // 4. 返回 TopK
        int limit = Math.min(topK > 0 ? topK : TOP_K_RERANK, candidates.size());
        return candidates.subList(0, limit);
    }

    /**
     * 检索并返回"可展示的来源"：文档名 / 片段序号 / 得分
     * @param query 用户问题
     * @param topK  取前 K 条
     */
    public List<Map<String, Object>> searchCitations(String query, int topK) {
        List<KnowledgeChunk> chunks = search(query, topK);
        if (chunks.isEmpty()) return List.of();

        Map<Long, String> titles = loadDocTitles();
        List<Map<String, Object>> out = new ArrayList<>();
        for (KnowledgeChunk c : chunks) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("docId", c.getDocId());
            m.put("title", titleOf(c.getDocId(), titles));
            m.put("chunkIndex", c.getChunkIndex());
            m.put("score", c.getSimilarityScore());
            out.add(m);
        }
        return out;
    }

    /** docId -> 文档名（一次查询，供上下文与引用共用） */
    private Map<Long, String> loadDocTitles() {
        Map<Long, String> map = new HashMap<>();
        for (KnowledgeDoc d : knowledgeDocMapper.findAll()) {
            map.put(d.getId(), d.getFileName());
        }
        return map;
    }

    /** 负数 docId 表示来自 notice 表的制度公告 */
    private String titleOf(Long docId, Map<Long, String> titles) {
        if (docId == null) return "未知来源";
        if (docId < 0) {
            Notice n = noticeMapper.getById((int) -docId.longValue());
            return n != null ? n.getTitle() : "公告";
        }
        return titles.getOrDefault(docId, "文档 #" + docId);
    }

    /**
     * 搜索结果的紧凑字符串拼接（用于注入 LLM Prompt）
     */
    public String searchToContext(String query, int topK) {
        List<KnowledgeChunk> chunks = search(query, topK);
        if (chunks.isEmpty()) return "";
        Map<Long, String> titles = loadDocTitles();
        return chunks.stream()
            .map(c -> "【" + titleOf(c.getDocId(), titles) + "】\n" + c.getContent().trim())
            .collect(Collectors.joining("\n\n"));
    }

    /**
     * Notice 表制度类搜索：转为 chunk 结构（过渡阶段）
     */
    private List<KnowledgeChunk> searchNoticeChunks(String query, List<String> keywords) {
        try {
            List<Notice> notices = noticeMapper.findByType(3); // type=3 是制度
            List<KnowledgeChunk> chunkList = new ArrayList<>();
            for (Notice n : notices) {
                String doc = (n.getTitle() != null ? n.getTitle() + "。" : "") +
                             (n.getContent() != null ? n.getContent() : "");
                if (doc.isEmpty()) continue;
                double score = rerankScore(doc, keywords);
                if (score > 0) {
                    KnowledgeChunk chunk = new KnowledgeChunk();
                    chunk.setDocId(-1L * n.getId()); // 负数 doc_id 表示来自 Notice 的特殊 chunk
                    chunk.setContent(doc.length() > 600 ? doc.substring(0, 600) + "..." : doc);
                    chunk.setSimilarityScore(score * 0.9); // Notice 候选轻微降权
                    chunkList.add(chunk);
                }
            }
            return chunkList;
        } catch (Exception e) {
            log.debug("Notice表检索失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 降级：纯 LIKE 拼接全文扫描（避免全文索引不可用）
     */
    private List<KnowledgeChunk> fallbackLikeSearch(List<String> keywords) {
        List<KnowledgeChunk> result = new ArrayList<>();
        try {
            List<KnowledgeDoc> docs = knowledgeDocMapper.findAll();
            for (KnowledgeDoc doc : docs) {
                if (!"READY".equals(doc.getStatus())) continue;
                List<KnowledgeChunk> chunks = knowledgeChunkMapper.findByDocId(doc.getId());
                for (KnowledgeChunk chunk : chunks) {
                    if (keywords.stream().anyMatch(k -> chunk.getContent().contains(k))) {
                        result.add(chunk);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("降级LIKE搜索失败: {}", e.getMessage());
        }
        return result;
    }

    /**
     * Re-rank 评分：关键词命中密度 + 句长加权
     * 分数 = Σ(关键词命中次数)/chunk字符数 × log(句长归一化+1)
     */
    private double rerankScore(String content, List<String> keywords) {
        if (content == null || content.isEmpty()) return 0;
        content = content.toLowerCase();
        int hitCount = 0;
        for (String kw : keywords) {
            int idx = 0;
            while ((idx = content.indexOf(kw, idx)) != -1) {
                hitCount++;
                idx += kw.length();
            }
        }
        double density = hitCount / (double) Math.max(content.length(), 1);
        double lengthBoost = Math.log1p(content.length() / 100.0);
        return density * lengthBoost * 1000;
    }

    /**
     * 简单中文分词：整句 + 连续中文片段的二元组
     * 只保留整句时，"学员请假管理制度是怎么规定的"这类长问句在 chunk 里找不到字面匹配，得分恒为 0。
     */
    private List<String> tokenize(String query) {
        if (query == null) return Collections.emptyList();
        List<String> tokens = new ArrayList<>();
        var matcher = Pattern.compile("[\\u4e00-\\u9fa5]{2,}").matcher(query);
        while (matcher.find()) {
            String run = matcher.group();
            tokens.add(run);
            if (run.length() > 3) {
                for (int i = 0; i + 2 <= run.length(); i++) {
                    tokens.add(run.substring(i, i + 2));
                }
            }
        }
        // 完整问句权重最高，放最前
        if (!tokens.contains(query)) tokens.add(0, query);
        return tokens.stream().distinct().limit(12).collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════
    //  文档管理
    // ════════════════════════════════════════════════════

    public List<KnowledgeDoc> listDocuments() {
        return knowledgeDocMapper.findAll();
    }

    public List<KnowledgeDoc> listByCategory(String category) {
        return knowledgeDocMapper.findByCategory(category);
    }

    public void deleteDocument(Long docId) {
        knowledgeChunkMapper.deleteByDocId(docId);
        knowledgeDocMapper.deleteById(docId);
        aiAnswerCache.clear();
        log.info("知识库文档已删除: docId={}", docId);
    }

    public void clearByCategory(String category) {
        List<KnowledgeDoc> docs = knowledgeDocMapper.findByCategory(category);
        for (KnowledgeDoc doc : docs) {
            deleteDocument(doc.getId());
        }
    }

    public boolean isAvailable() {
        return knowledgeDocMapper != null;
    }

    // ════════════════════════════════════════════════════
    //  辅助
    // ════════════════════════════════════════════════════

    /** 粗略 token 估算：中文字 × 1.2 + 标点 × 0.3 */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return Math.max(1, (int) (text.replaceAll("[\\u4e00-\\u9fa5]", "中").length() * 0.6));
    }
}
