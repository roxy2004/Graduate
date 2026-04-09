package com.zjut.graduate.Dao;

import com.zjut.graduate.Po.MistakeAnalysis;
import org.apache.ibatis.annotations.*;

@Mapper
public interface MistakeAnalysisDao {
    @Select("SELECT * FROM mistake_analysis WHERE id = #{id}")
    MistakeAnalysis selectById(Long id);

    @Select("SELECT * FROM mistake_analysis WHERE record_id = #{recordId}")
    MistakeAnalysis selectByRecordId(Long recordId);

    @Insert("INSERT INTO mistake_analysis (record_id, knowledge_point, error_type, suggestion, raw_llm_output, created_at) " +
            "VALUES (#{recordId}, #{knowledgePoint}, #{errorType}, #{suggestion}, #{rawLlmOutput}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MistakeAnalysis analysis);

    @Update("UPDATE mistake_analysis SET knowledge_point = #{knowledgePoint}, error_type = #{errorType}, " +
            "suggestion = #{suggestion}, raw_llm_output = #{rawLlmOutput} WHERE id = #{id}")
    int update(MistakeAnalysis analysis);
}
