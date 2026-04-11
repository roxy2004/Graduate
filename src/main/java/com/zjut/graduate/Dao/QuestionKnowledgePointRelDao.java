package com.zjut.graduate.Dao;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QuestionKnowledgePointRelDao {

    @Insert("INSERT INTO question_knowledge_point_rel (question_id, kp_id, weight, created_at) " +
            "VALUES (#{questionId}, #{kpId}, 1.0, NOW())")
    int insert(@Param("questionId") Long questionId, @Param("kpId") Long kpId);

    @Delete("DELETE FROM question_knowledge_point_rel WHERE question_id = #{questionId}")
    int deleteByQuestionId(@Param("questionId") Long questionId);

    @Select("SELECT COUNT(1) FROM question_knowledge_point_rel WHERE question_id = #{questionId} AND kp_id = #{kpId}")
    int countByQuestionAndKp(@Param("questionId") Long questionId, @Param("kpId") Long kpId);
}
