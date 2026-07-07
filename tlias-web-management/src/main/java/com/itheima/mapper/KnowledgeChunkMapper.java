package com.itheima.mapper;

import com.itheima.pojo.KnowledgeChunk;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeChunkMapper {

    /**
     * 批量插入 chunk（jdbcType=ARRAY 需 Spring Boot 3 适配，文中先用单条）
     */
    @Insert("INSERT INTO tlias_knowledge_chunk (doc_id, chunk_index, content, token_count, embedding_json, create_time) " +
            "VALUES (#{docId}, #{chunkIndex}, #{content}, #{tokenCount}, " +
            "CAST(#{embeddingJson} AS JSON), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(@Param("docId") Long docId,
               @Param("chunkIndex") Integer chunkIndex,
               @Param("content") String content,
               @Param("tokenCount") Integer tokenCount,
               @Param("embeddingJson") String embeddingJson);

    @Select("SELECT id, doc_id, chunk_index, content, token_count, embedding_json " +
            "FROM tlias_knowledge_chunk WHERE doc_id = #{docId} ORDER BY chunk_index")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "docId", column = "doc_id"),
        @Result(property = "chunkIndex", column = "chunk_index"),
        @Result(property = "content", column = "content"),
        @Result(property = "tokenCount", column = "token_count"),
        @Result(property = "embeddingJson", column = "embedding_json")
    })
    List<KnowledgeChunk> findByDocId(Long docId);

    /**
     * TopK 文本检索（LIKE 子串匹配 + 全文索引兜底，按热度评分排序）
     * 这是过渡实现：在 Spring AI GA 版可用后切换 VectorStore
     */
    @Select("""
            SELECT c.id, c.doc_id, c.chunk_index, c.content, c.token_count,
                   MATCH(c.content) AGAINST(#{query} IN NATURAL LANGUAGE MODE) AS score
            FROM tlias_knowledge_chunk c
            LEFT JOIN tlias_knowledge_doc d ON c.doc_id = d.id
            WHERE d.status = 'READY'
              AND (
                MATCH(c.content) AGAINST(#{query} IN NATURAL LANGUAGE MODE)
                OR c.content LIKE CONCAT('%', #{queryLike}, '%')
              )
            ORDER BY score DESC, c.id ASC
            LIMIT #{limit}
            """)
    List<KnowledgeChunk> searchFullText(@Param("query") String query,
                                        @Param("queryLike") String queryLike,
                                        @Param("limit") int limit);

    /** 删除某文档的所有 chunk */
    @Delete("DELETE FROM tlias_knowledge_chunk WHERE doc_id = #{docId}")
    int deleteByDocId(Long docId);

    /** 统计文档块数 */
    @Select("SELECT COUNT(*) FROM tlias_knowledge_chunk WHERE doc_id = #{docId}")
    int countByDocId(Long docId);
}
