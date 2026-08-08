package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库文档实体
 * 对应表：tlias_knowledge_doc
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDoc {
    private Long id;
    private String fileName;
    private String category;       // policy/course/faq/teacher/student
    private Integer chunkCount;
    private String status;         // PROCESSING/READY/FAILED
    private String filePath;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
