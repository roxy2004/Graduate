package com.zjut.graduate.Dao;

import com.zjut.graduate.Po.MistakeAnalysis;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MistakeAnalysisDao {
    @Select("SELECT * FROM mistake_analysis WHERE id = #{id}")
    MistakeAnalysis selectById(Long id);

    @Select("SELECT * FROM mistake_analysis WHERE record_id = #{recordId}")
    MistakeAnalysis selectByRecordId(Long recordId);

    @Insert("INSERT INTO mistake_analysis (record_id, user_id, kp_id, error_type, weakness_score, suggestion, raw_llm_output, created_at, updated_at) " +
            "VALUES (#{recordId}, (SELECT user_id FROM learning_record WHERE id = #{recordId}), NULL, #{errorType}, 0.60, " +
            "#{suggestion}, #{rawLlmOutput}, #{createdAt}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MistakeAnalysis analysis);

    @Update("UPDATE mistake_analysis SET error_type = #{errorType}, suggestion = #{suggestion}, " +
            "raw_llm_output = #{rawLlmOutput}, updated_at = NOW() WHERE id = #{id}")
    int update(MistakeAnalysis analysis);

    /**
     * 删除某题对应练习记录上的错题分析（先于 learning_record 删除）。
     */
    @Delete("DELETE ma FROM mistake_analysis ma " +
            "INNER JOIN learning_record lr ON lr.id = ma.record_id " +
            "WHERE lr.question_id = #{questionId}")
    int deleteByQuestionId(@Param("questionId") Long questionId);
}
