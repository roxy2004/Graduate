package com.zjut.graduate.Dao;

import com.zjut.graduate.Po.QuestionBank;
import org.apache.ibatis.annotations.*;

import java.util.List;
@Mapper
public interface QuestionBankDao {
    @Select("SELECT * FROM question_bank WHERE id = #{id}")
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

    @Select("SELECT * FROM question_bank ORDER BY created_at DESC")
    @ResultMap("questionResult")
    List<QuestionBank> selectAll();

    /**
     * 根据知识点ID列表筛选题目（任意一个知识点匹配即可）
     * 逗号分隔的知识点ID字符串，如 "1,3"
     * @return 匹配的题目列表
     */
    @Select("<script>" +
            "SELECT * FROM question_bank WHERE " +
            "<foreach collection='kpIdsArray' item='kpId' open='' separator=' OR ' close=''>" +
            "FIND_IN_SET(#{kpId}, knowledge_point_ids) > 0" +
            "</foreach>" +
            "</script>")
    List<QuestionBank> selectByKpIds(@Param("kpIdsArray") Long[] kpIdsArray);

    @Insert("INSERT INTO question_bank (content, image_url, options, correct_answer, difficulty, knowledge_point_ids, created_at) " +
            "VALUES (#{content}, #{imageUrl}, #{options}, #{correctAnswer}, #{difficulty}, #{knowledgePointIds}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuestionBank question);

    @Update("UPDATE question_bank SET content = #{content}, image_url = #{imageUrl}, options = #{options}, " +
            "correct_answer = #{correctAnswer}, difficulty = #{difficulty}, knowledge_point_ids = #{knowledgePointIds} " +
            "WHERE id = #{id}")
    int update(QuestionBank question);

    @Delete("DELETE FROM question_bank WHERE id = #{id}")
    int deleteById(Long id);
}
