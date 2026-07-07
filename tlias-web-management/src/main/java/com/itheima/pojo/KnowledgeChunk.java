package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库文档分块实体
 * 表：tlias_knowledge_chunk
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeChunk {
    private Long id;
    private Long docId;
    private Integer chunkIndex;
    private String content;
    private Integer tokenCount;
    private List<Double> embedding;

    /** 本次查询时的相似度得分（不落库，仅用于排序/调试） */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private transient double similarityScore;

    private LocalDateTime createTime;
}
