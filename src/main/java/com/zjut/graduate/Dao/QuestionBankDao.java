package com.zjut.graduate.Dao;

import com.zjut.graduate.Po.QuestionBank;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface QuestionBankDao {
    @Select("SELECT qb.id, qb.content, qb.question_type, qb.source_tag, qb.status, NULL AS image_url, qb.options, qb.correct_answer, qb.difficulty, " +
            "(SELECT GROUP_CONCAT(rel.kp_id ORDER BY rel.kp_id SEPARATOR ',') " +
            " FROM question_knowledge_point_rel rel WHERE rel.question_id = qb.id) AS knowledge_point_ids, " +
            "qb.created_at " +
            "FROM question_bank qb WHERE qb.id = #{id}")
    @Results(id = "questionResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "content", property = "content"),
            @Result(column = "question_type", property = "questionType"),
            @Result(column = "source_tag", property = "sourceTag"),
            @Result(column = "status", property = "status"),
            @Result(column = "image_url", property = "imageUrl"),
            @Result(column = "options", property = "options"),
            @Result(column = "correct_answer", property = "correctAnswer"),
            @Result(column = "difficulty", property = "difficulty"),
            @Result(column = "knowledge_point_ids", property = "knowledgePointIds"),
            @Result(column = "created_at", property = "createdAt")
    })
    QuestionBank selectById(Long id);

    @Select("SELECT qb.id, qb.content, qb.question_type, qb.source_tag, qb.status, NULL AS image_url, qb.options, qb.correct_answer, qb.difficulty, " +
            "(SELECT GROUP_CONCAT(rel.kp_id ORDER BY rel.kp_id SEPARATOR ',') " +
            " FROM question_knowledge_point_rel rel WHERE rel.question_id = qb.id) AS knowledge_point_ids, " +
            "qb.created_at " +
            "FROM question_bank qb ORDER BY qb.created_at DESC")
    @ResultMap("questionResult")
    List<QuestionBank> selectAll();

    /**
     * 根据知识点ID列表筛选题目（任意一个知识点匹配即可）
     */
    @Select("<script>" +
            "SELECT DISTINCT qb.id, qb.content, qb.question_type, qb.source_tag, qb.status, NULL AS image_url, qb.options, qb.correct_answer, qb.difficulty, " +
            "(SELECT GROUP_CONCAT(rel2.kp_id ORDER BY rel2.kp_id SEPARATOR ',') " +
            " FROM question_knowledge_point_rel rel2 WHERE rel2.question_id = qb.id) AS knowledge_point_ids, " +
            "qb.created_at " +
            "FROM question_bank qb " +
            "INNER JOIN question_knowledge_point_rel rel ON rel.question_id = qb.id " +
            "WHERE rel.kp_id IN " +
            "<foreach collection='kpIdsArray' item='kpId' open='(' separator=',' close=')'>" +
            "#{kpId}" +
            "</foreach>" +
            " ORDER BY qb.created_at DESC" +
            "</script>")
    @ResultMap("questionResult")
    List<QuestionBank> selectByKpIds(@Param("kpIdsArray") Long[] kpIdsArray);

    @Insert("INSERT INTO question_bank (content, question_type, options, correct_answer, explanation, difficulty, " +
            "discrimination, guess_prob, cognitive_level, source_tag, source_url, quality_score, status, created_by, created_at, updated_at) " +
            "VALUES (#{content}, #{questionType}, #{options}, #{correctAnswer}, NULL, #{difficulty}, " +
            "0.50, 0.25, 'understand', #{sourceTag}, NULL, 0.70, #{status}, #{createdBy}, #{createdAt}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuestionBank question);

    @Update("UPDATE question_bank SET content = #{content}, options = #{options}, " +
            "correct_answer = #{correctAnswer}, difficulty = #{difficulty}, updated_at = NOW() " +
            "WHERE id = #{id}")
    int update(QuestionBank question);

    @Delete("DELETE FROM question_bank WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 下一题：该知识点下未做过优先；均做过则按最近一次作答时间升序轮询。
     */
    @Select("SELECT qb.id, qb.content, qb.question_type, qb.source_tag, qb.status, NULL AS image_url, qb.options, qb.correct_answer, qb.difficulty, " +
            "(SELECT GROUP_CONCAT(rel2.kp_id ORDER BY rel2.kp_id SEPARATOR ',') " +
            " FROM question_knowledge_point_rel rel2 WHERE rel2.question_id = qb.id) AS knowledge_point_ids, " +
            "qb.created_at " +
            "FROM question_bank qb " +
            "INNER JOIN question_knowledge_point_rel rel ON rel.question_id = qb.id AND rel.kp_id = #{kpId} " +
            "WHERE qb.status = 1 " +
            "ORDER BY " +
            "  (SELECT COUNT(*) FROM learning_record lr WHERE lr.user_id = #{userId} AND lr.question_id = qb.id) ASC, " +
            "  (SELECT MAX(lr.answered_at) FROM learning_record lr " +
            "   WHERE lr.user_id = #{userId} AND lr.question_id = qb.id) ASC, " +
            "  qb.id ASC " +
            "LIMIT 1")
    @ResultMap("questionResult")
    QuestionBank selectNextPracticeQuestion(@Param("kpId") Long kpId, @Param("userId") Long userId);

    /**
     * 练习卡片条：该知识点全部题目 + 用户最近一次作答（未做优先，已做按最近作答时间升序）。
     */
    @Select("SELECT qb.id, qb.content, qb.question_type AS questionType, qb.options, qb.difficulty, qb.source_tag AS sourceTag, " +
            "qb.correct_answer AS correctAnswer, " +
            "lrj.user_answer AS lastUserAnswer, lrj.is_correct AS lastIsCorrect, lrj.answered_at AS lastAnsweredAt, " +
            "lrj.time_spent AS lastTimeSpent " +
            "FROM question_bank qb " +
            "INNER JOIN question_knowledge_point_rel rel ON rel.question_id = qb.id AND rel.kp_id = #{kpId} " +
            "LEFT JOIN ( " +
            "  SELECT t.question_id, t.user_answer, t.is_correct, t.answered_at, t.time_spent " +
            "  FROM ( " +
            "    SELECT lr.question_id, lr.user_answer, lr.is_correct, lr.answered_at, lr.time_spent, " +
            "           ROW_NUMBER() OVER (PARTITION BY lr.question_id ORDER BY lr.answered_at DESC, lr.id DESC) AS rn " +
            "    FROM learning_record lr WHERE lr.user_id = #{userId} " +
            "  ) t WHERE t.rn = 1 " +
            ") lrj ON lrj.question_id = qb.id " +
            "WHERE qb.status = 1 " +
            "ORDER BY (lrj.answered_at IS NULL) DESC, lrj.answered_at ASC, qb.id ASC")
    List<Map<String, Object>> selectPracticeDeckRows(@Param("kpId") Long kpId, @Param("userId") Long userId);
}
