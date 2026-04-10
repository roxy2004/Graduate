package com.zjut.graduate.Dao;

import com.zjut.graduate.Po.QuestionBank;
import org.apache.ibatis.annotations.*;

import java.util.List;
@Mapper
public interface QuestionBankDao {
    @Select("SELECT qb.id, qb.content, NULL AS image_url, qb.options, qb.correct_answer, qb.difficulty, " +
            "(SELECT GROUP_CONCAT(rel.kp_id ORDER BY rel.kp_id SEPARATOR ',') " +
            " FROM question_knowledge_point_rel rel WHERE rel.question_id = qb.id) AS knowledge_point_ids, " +
            "qb.created_at " +
            "FROM question_bank qb WHERE qb.id = #{id}")
    @Results(id = "questionResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "content", property = "content"),
            @Result(column = "image_url", property = "imageUrl"),
            @Result(column = "options", property = "options"),
            @Result(column = "correct_answer", property = "correctAnswer"),
            @Result(column = "difficulty", property = "difficulty"),
            @Result(column = "knowledge_point_ids", property = "knowledgePointIds"),
            @Result(column = "created_at", property = "createdAt")
    })
    QuestionBank selectById(Long id);

    @Select("SELECT qb.id, qb.content, NULL AS image_url, qb.options, qb.correct_answer, qb.difficulty, " +
            "(SELECT GROUP_CONCAT(rel.kp_id ORDER BY rel.kp_id SEPARATOR ',') " +
            " FROM question_knowledge_point_rel rel WHERE rel.question_id = qb.id) AS knowledge_point_ids, " +
            "qb.created_at " +
            "FROM question_bank qb ORDER BY qb.created_at DESC")
    @ResultMap("questionResult")
    List<QuestionBank> selectAll();

    /**
     * 根据知识点ID列表筛选题目（任意一个知识点匹配即可）
     * 逗号分隔的知识点ID字符串，如 "1,3"
     * @return 匹配的题目列表
     */
    @Select("<script>" +
            "SELECT DISTINCT qb.id, qb.content, NULL AS image_url, qb.options, qb.correct_answer, qb.difficulty, " +
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
    List<QuestionBank> selectByKpIds(@Param("kpIdsArray") Long[] kpIdsArray);

    @Insert("INSERT INTO question_bank (content, question_type, options, correct_answer, explanation, difficulty, " +
            "discrimination, guess_prob, cognitive_level, source_tag, quality_score, status, created_at, updated_at) " +
            "VALUES (#{content}, 'choice', #{options}, #{correctAnswer}, NULL, #{difficulty}, " +
            "0.50, 0.25, 'understand', '教师导入', 0.70, 1, #{createdAt}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuestionBank question);

    @Update("UPDATE question_bank SET content = #{content}, options = #{options}, " +
            "correct_answer = #{correctAnswer}, difficulty = #{difficulty}, updated_at = NOW() " +
            "WHERE id = #{id}")
    int update(QuestionBank question);

    @Delete("DELETE FROM question_bank WHERE id = #{id}")
    int deleteById(Long id);
}
