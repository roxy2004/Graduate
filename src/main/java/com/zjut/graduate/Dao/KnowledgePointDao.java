package com.zjut.graduate.Dao;

import com.zjut.graduate.Po.KnowledgePoint;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface KnowledgePointDao {
    @Select("SELECT * FROM knowledge_point WHERE id = #{id}")
    KnowledgePoint selectById(Long id);

    /**
     * 按知识点名与小节标题模糊匹配：精确相等优先，其次小节标题包含知识点名，再次知识点名包含小节标题。
     */
    @Select("SELECT s.id FROM course_section s " +
            "INNER JOIN knowledge_point kp ON kp.id = #{kpId} " +
            "WHERE s.is_active = 1 " +
            "AND TRIM(IFNULL(kp.name, '')) <> '' " +
            "AND TRIM(IFNULL(s.title, '')) <> '' " +
            "AND (s.title = TRIM(kp.name) " +
            "OR s.title LIKE CONCAT('%', TRIM(kp.name), '%') " +
            "OR TRIM(kp.name) LIKE CONCAT('%', TRIM(s.title), '%')) " +
            "ORDER BY CASE WHEN s.title = TRIM(kp.name) THEN 0 " +
            "WHEN s.title LIKE CONCAT('%', TRIM(kp.name), '%') THEN 1 ELSE 2 END, " +
            "CHAR_LENGTH(TRIM(s.title)) ASC, s.id ASC LIMIT 1")
    Long selectSectionIdByKnowledgePointTitleMatch(@Param("kpId") Long kpId);

    @Select("SELECT id FROM knowledge_point WHERE name = #{name} ORDER BY id ASC LIMIT 1")
    Long selectIdByExactName(@Param("name") String name);

    /**
     * CSV 导入：按固定 id 创建或更新知识点（满足 question_knowledge_point_rel 外键）。
     */
    @Insert("INSERT INTO knowledge_point (id, name, category, description, difficulty_ref, created_at, updated_at) " +
            "VALUES (#{id}, #{name}, '教师导入', 'CSV批量导入自动创建', 0.50, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE name = VALUES(name), category = VALUES(category), " +
            "description = VALUES(description), difficulty_ref = VALUES(difficulty_ref), updated_at = NOW()")
    int upsertById(@Param("id") Long id, @Param("name") String name);

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

    /**
     * 知识点学习时长达成率（actual/target）：
     * - target 秒数来源：绑定小节 estimated_minutes * 60（最小按 60 秒兜底）；
     * - actual 秒数来源：user_learning_progress.total_seconds；
     * - 若未配置 anchor_section_id，尝试用知识点名与小节标题做一次最佳匹配。
     */
    @Select("SELECT kp.id AS kpId, " +
            "COALESCE(ulp.total_seconds, 0) AS learnedSeconds, " +
            "GREATEST(60, COALESCE(cs.estimated_minutes, 0) * 60) AS targetSeconds, " +
            "LEAST(1.5, COALESCE(ulp.total_seconds, 0) / GREATEST(60, COALESCE(cs.estimated_minutes, 0) * 60)) AS timeRatio " +
            "FROM knowledge_point kp " +
            "LEFT JOIN course_section cs ON cs.id = COALESCE(kp.anchor_section_id, (" +
            "  SELECT s2.id FROM course_section s2 " +
            "  WHERE s2.is_active = 1 " +
            "    AND TRIM(IFNULL(kp.name, '')) <> '' " +
            "    AND TRIM(IFNULL(s2.title, '')) <> '' " +
            "    AND (s2.title = TRIM(kp.name) " +
            "      OR s2.title LIKE CONCAT('%', TRIM(kp.name), '%') " +
            "      OR TRIM(kp.name) LIKE CONCAT('%', TRIM(s2.title), '%')) " +
            "  ORDER BY CASE WHEN s2.title = TRIM(kp.name) THEN 0 " +
            "    WHEN s2.title LIKE CONCAT('%', TRIM(kp.name), '%') THEN 1 ELSE 2 END, " +
            "    CHAR_LENGTH(TRIM(s2.title)) ASC, s2.id ASC LIMIT 1" +
            ")) " +
            "LEFT JOIN user_learning_progress ulp ON ulp.user_id = #{userId} AND ulp.section_id = cs.id")
    List<Map<String, Object>> selectLearningTimeRatiosByUser(@Param("userId") Long userId);
}
