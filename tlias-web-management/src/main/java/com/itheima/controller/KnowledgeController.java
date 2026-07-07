package com.itheima.controller;

import com.itheima.ai.rag.KnowledgeBaseService;
import com.itheima.pojo.KnowledgeChunk;
import com.itheima.pojo.KnowledgeDoc;
import com.itheima.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 知识库管理控制器（Phase 2 重构版）
 *
 * 使用新的 KnowledgeBaseService.search() 走 FULLTEXT + Re-rank
 */
@Slf4j
@RequestMapping("/knowledge")
@RestController
public class KnowledgeController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @PostMapping("/upload")
    public Result uploadDocument(@RequestParam("file") MultipartFile file,
                                  @RequestParam(value = "category", defaultValue = "faq") String category) {
        if (file.isEmpty()) return Result.error(400, "文件不能为空");
        String fileName = file.getOriginalFilename();
        if (fileName == null) return Result.error(400, "文件名不能为空");

        try {
            Long docId = knowledgeBaseService.uploadDocument(file, category);
            KnowledgeDoc doc = knowledgeBaseService.listDocuments().stream()
                    .filter(d -> d.getId().equals(docId)).findFirst().orElse(null);
            return Result.success(Map.of(
                    "id", docId,
                    "fileName", fileName,
                    "category", category,
                    "status", doc != null ? doc.getStatus() : "PROCESSING"
            ));
        } catch (Exception e) {
            log.error("知识库文档上传失败", e);
            return Result.error(500, "文档处理失败: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public Result search(@RequestParam("q") String query) {
        if (query == null || query.trim().isEmpty()) return Result.error(400, "查询不能为空");
        List<KnowledgeChunk> results = knowledgeBaseService.search(query, 3);
        return Result.success(results.stream().map(c -> Map.of(
                "docId", c.getDocId() != null ? c.getDocId() : 0,
                "chunkIndex", c.getChunkIndex() != null ? c.getChunkIndex() : 0,
                "content", c.getContent() != null ? c.getContent().substring(0, Math.min(c.getContent().length(), 500)) : "",
                "score", c.getSimilarityScore()
        )).toList());
    }

    @GetMapping("/documents")
    public Result listDocuments() {
        return Result.success(knowledgeBaseService.listDocuments());
    }

    @DeleteMapping("/documents/{id}")
    public Result deleteDocument(@PathVariable Long id) {
        knowledgeBaseService.deleteDocument(id);
        return Result.success("文档已删除");
    }

    @GetMapping("/status")
    public Result status() {
        return Result.success(Map.of(
                "available", knowledgeBaseService.isAvailable(),
                "documentCount", knowledgeBaseService.listDocuments().size()
        ));
    }
}
