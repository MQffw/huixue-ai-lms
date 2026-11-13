package com.huixuetong.controller;

import com.huixuetong.ai.rag.KnowledgeBaseService;
import com.huixuetong.pojo.KnowledgeChunk;
import com.huixuetong.pojo.KnowledgeDoc;
import com.huixuetong.pojo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "知识库管理", description = "知识库文档上传、检索与管理接口")
public class KnowledgeController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Operation(summary = "上传知识库文档")
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
                    "fileName", doc != null ? doc.getFileName() : fileName,
                    "category", category,
                    "status", doc != null ? doc.getStatus() : "PROCESSING"
            ));
        } catch (Exception e) {
            log.error("知识库文档上传失败", e);
            return Result.error(500, "文档处理失败: " + e.getMessage());
        }
    }

    @Operation(summary = "检索知识库")
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

    @Operation(summary = "查询文档列表")
    @GetMapping("/documents")
    public Result listDocuments() {
        return Result.success(knowledgeBaseService.listDocuments());
    }

    @Operation(summary = "删除文档")
    @DeleteMapping("/documents/{id}")
    public Result deleteDocument(@PathVariable Long id) {
        knowledgeBaseService.deleteDocument(id);
        return Result.success("文档已删除");
    }

    @Operation(summary = "查询知识库状态")
    @GetMapping("/status")
    public Result status() {
        return Result.success(Map.of(
                "available", knowledgeBaseService.isAvailable(),
                "documentCount", knowledgeBaseService.listDocuments().size()
        ));
    }
}