package com.zjut.graduate.Dao;

import com.zjut.graduate.Po.KnowledgePoint;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface KnowledgePointDao {
    @Select("SELECT * FROM knowledge_point WHERE id = #{id}")
    KnowledgePoint selectById(Long id);

    @Select("SELECT * FROM knowledge_point")
    List<KnowledgePoint> selectAll();

    @Insert("INSERT INTO knowledge_point (name, category, description, difficulty_ref, created_at, updated_at) " +
            "VALUES (#{name}, '通用', '', 0.50, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgePoint kp);

    @Update("UPDATE knowledge_point SET name = #{name} WHERE id = #{id}")
    int update(KnowledgePoint kp);

    @Delete("DELETE FROM knowledge_point WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 练习中心：仅返回「有关联题目」的知识点及总题数、已刷题数（去重题目）。
     */
    @Select("SELECT kp.id, kp.name, " +
            "  (SELECT COUNT(DISTINCT qkr.question_id) FROM question_knowledge_point_rel qkr " +
            "   INNER JOIN question_bank qb ON qb.id = qkr.question_id AND qb.status = 1 WHERE qkr.kp_id = kp.id) AS totalQuestions, " +
            "  (SELECT COUNT(DISTINCT lr.question_id) FROM learning_record lr " +
            "   INNER JOIN question_knowledge_point_rel qkr2 ON qkr2.question_id = lr.question_id AND qkr2.kp_id = kp.id " +
            "   WHERE lr.user_id = #{userId}) AS practicedQuestions " +
            "FROM knowledge_point kp " +
            "WHERE (SELECT COUNT(DISTINCT qkr.question_id) FROM question_knowledge_point_rel qkr " +
            "       INNER JOIN question_bank qb ON qb.id = qkr.question_id AND qb.status = 1 WHERE qkr.kp_id = kp.id) > 0 " +
            "ORDER BY kp.id")
    List<Map<String, Object>> selectPracticeSummaryByUser(@Param("userId") Long userId);
}
