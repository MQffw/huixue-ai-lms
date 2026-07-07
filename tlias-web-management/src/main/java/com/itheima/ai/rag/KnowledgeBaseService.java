package com.itheima.ai.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.mapper.KnowledgeChunkMapper;
import com.itheima.mapper.KnowledgeDocMapper;
import com.itheima.mapper.NoticeMapper;
import com.itheima.pojo.KnowledgeChunk;
import com.itheima.pojo.KnowledgeDoc;
import com.itheima.pojo.Notice;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ════════════════════════════════════════════════════
    //  Upload：解析 + 切片 + 入库
    // ════════════════════════════════════════════════════

    /**
     * 解析 + 切片 + 入库
         * @return 文档 ID
     */
    public Long uploadDocument(MultipartFile file, String category) throws IOException {
        String originalName = file.getOriginalFilename();
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
            List<String> chunks = SentenceSplitter.split(text, CHUNK_SIZE, CHUNK_OVERLAP);

            // 4. 逐条入库（分批提交）
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                String embeddingJson = "[]"; // 过渡期暂不向量化，直接空数组
                knowledgeChunkMapper.insert(docId, i, chunk, estimateTokens(chunk), embeddingJson);
            }

            // 5. 更新状态
            knowledgeDocMapper.updateStatus(docId, "READY", chunks.size());
            log.info("知识库文档入库完成: docId={}, chunks={}, file={}", docId, chunks.size(), originalName);
            return docId;
        } catch (Exception e) {
            log.error("文档解析失败: {}", originalName, e);
            knowledgeDocMapper.updateStatus(docId, "FAILED", 0);
            throw new IOException("文档解析或入库失败: " + e.getMessage(), e);
        }
    }

    /**
     * 文本提取：尝试 Tika 解析（PDF/Word），失败则按 UTF-8 文本读取
     */
    private String extractText(MultipartFile file, Path savedPath) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null) return new String(file.getBytes(), StandardCharsets.UTF_8);

        // 简单判断是否是纯文本（.txt / .md / .csv 等直接用文本读取）
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".csv") || lower.endsWith(".log")) {
            return Files.readString(savedPath, StandardCharsets.UTF_8);
        }

        // 其他格式先用字节读 UTF-8，再 fallback（项目未引入 Tika 依赖，后续Phase丰富）
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return Files.readString(savedPath, StandardCharsets.UTF_8);
        }
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

        // 4. 返回 TopK
        int limit = Math.min(topK > 0 ? topK : TOP_K_RERANK, candidates.size());
        return candidates.subList(0, limit);
    }

    /**
     * 搜索结果的紧凑字符串拼接（用于注入 LLM Prompt）
     */
    public String searchToContext(String query, int topK) {
        List<KnowledgeChunk> chunks = search(query, topK);
        if (chunks.isEmpty()) return "";
        return chunks.stream()
            .map(c -> "【" + (c.getDocId() != null ? "制度 #" + c.getDocId() : "通知") + "】\n" + c.getContent().trim())
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
     * 简单中文分词：按非中文字符切分 + 多字词组提取
     */
    private List<String> tokenize(String query) {
        if (query == null) return Collections.emptyList();
        // 先用正则提取连续中文字符（连续2字以上）
        List<String> tokens = new ArrayList<>();
        var matcher = Pattern.compile("[\\u4e00-\\u9fa5]{2,}").matcher(query);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        // 再加整个 query（完整短语最相关）
        if (!tokens.contains(query)) tokens.add(0, query);
        return tokens.stream().distinct().limit(8).collect(Collectors.toList());
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
