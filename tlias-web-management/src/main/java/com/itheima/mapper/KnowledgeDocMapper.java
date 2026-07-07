package com.itheima.mapper;

import com.itheima.pojo.KnowledgeDoc;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeDocMapper {

    @Insert("INSERT INTO tlias_knowledge_doc (file_name, category, chunk_count, status, file_path, create_time, update_time) " +
            "VALUES (#{fileName}, #{category}, #{chunkCount}, #{status}, #{filePath}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeDoc doc);

    @Update("UPDATE tlias_knowledge_doc SET status = #{status}, chunk_count = #{chunkCount}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("chunkCount") Integer chunkCount);

    @Select("SELECT id, file_name, category, chunk_count, status, file_path, create_time, update_time FROM tlias_knowledge_doc WHERE category = #{category}")
    List<KnowledgeDoc> findByCategory(String category);

    @Select("SELECT id, file_name, category, chunk_count, status, file_path, create_time, update_time FROM tlias_knowledge_doc ORDER BY create_time DESC")
    List<KnowledgeDoc> findAll();

    @Delete("DELETE FROM tlias_knowledge_doc WHERE id = #{id}")
    int deleteById(Long id);
}
