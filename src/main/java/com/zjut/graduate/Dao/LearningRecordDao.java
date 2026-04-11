package com.zjut.graduate.Dao;

import com.zjut.graduate.Po.LearningRecord;
import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
@Mapper
public interface LearningRecordDao {
    @Select("SELECT * FROM learning_record WHERE id = #{id}")
    LearningRecord selectById(Long id);

    @Select("SELECT * FROM learning_record WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<LearningRecord> selectByUserId(Long userId);

    /**
     * 根据用户ID和时间范围查询（动态条件）
     */
    @Select("<script>" +
            "SELECT * FROM learning_record WHERE user_id = #{userId} " +
            "<if test='startTime != null'> AND created_at &gt;= #{startTime} </if>" +
            "<if test='endTime != null'> AND created_at &lt;= #{endTime} </if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    List<LearningRecord> selectByUserIdAndTime(@Param("userId") Long userId,
                                               @Param("startTime") Date startTime,
                                               @Param("endTime") Date endTime);

    @Insert("INSERT INTO learning_record (user_id, question_id, user_answer, is_correct, score, time_spent, attempt_no, answered_at, created_at) " +
            "VALUES (#{userId}, #{questionId}, #{userAnswer}, #{isCorrect}, " +
            "CASE WHEN #{isCorrect} = 1 THEN 5.00 ELSE 0.00 END, #{timeSpent}, 1, #{createdAt}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LearningRecord record);

    @Select("SELECT COUNT(1) FROM learning_record WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(1) FROM learning_record WHERE user_id = #{userId} AND is_correct = 1")
    int countCorrectByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(DISTINCT DATE(created_at)) FROM learning_record WHERE user_id = #{userId}")
    int countLearningDaysByUserId(@Param("userId") Long userId);

    @Select("SELECT lr.id, lr.question_id, qb.content, lr.user_answer, lr.is_correct, lr.time_spent, lr.created_at " +
            "FROM learning_record lr LEFT JOIN question_bank qb ON lr.question_id = qb.id " +
            "WHERE lr.user_id = #{userId} ORDER BY lr.created_at DESC")
    List<Map<String, Object>> selectWithQuestionContentByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM learning_record WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Delete("DELETE FROM learning_record WHERE user_id = #{userId}")
    int deleteAllByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM learning_record WHERE question_id = #{questionId}")
    int deleteByQuestionId(@Param("questionId") Long questionId);

    @Select("SELECT COALESCE(MAX(attempt_no), 0) FROM learning_record " +
            "WHERE user_id = #{userId} AND question_id = #{questionId}")
    int selectMaxAttemptNo(@Param("userId") Long userId, @Param("questionId") Long questionId);

    @Insert("INSERT INTO learning_record (user_id, question_id, user_answer, is_correct, score, time_spent, attempt_no, answered_at, created_at) " +
            "VALUES (#{userId}, #{questionId}, #{userAnswer}, #{isCorrect}, " +
            "CASE WHEN #{isCorrect} = 1 THEN 5.00 ELSE 0.00 END, #{timeSpent}, #{attemptNo}, NOW(), NOW())")
    int insertPracticeAttempt(@Param("userId") Long userId,
                              @Param("questionId") Long questionId,
                              @Param("userAnswer") String userAnswer,
                              @Param("isCorrect") int isCorrect,
                              @Param("timeSpent") int timeSpent,
                              @Param("attemptNo") int attemptNo);

    @Delete("DELETE ma FROM mistake_analysis ma " +
            "INNER JOIN learning_record lr ON lr.id = ma.record_id " +
            "INNER JOIN question_knowledge_point_rel qkr ON qkr.question_id = lr.question_id AND qkr.kp_id = #{kpId} " +
            "WHERE lr.user_id = #{userId}")
    int deleteMistakeAnalysisByUserAndKp(@Param("userId") Long userId, @Param("kpId") Long kpId);

    @Delete("DELETE lr FROM learning_record lr " +
            "INNER JOIN question_knowledge_point_rel qkr ON qkr.question_id = lr.question_id AND qkr.kp_id = #{kpId} " +
            "WHERE lr.user_id = #{userId}")
    int deleteLearningRecordsByUserAndKp(@Param("userId") Long userId, @Param("kpId") Long kpId);

    @Select("SELECT lr.id, lr.question_id, qb.content, qb.options, qb.correct_answer, lr.user_answer, lr.is_correct, " +
            "ma.error_type, kp.name AS knowledge_point, lr.created_at " +
            "FROM learning_record lr " +
            "LEFT JOIN question_bank qb ON lr.question_id = qb.id " +
            "LEFT JOIN mistake_analysis ma ON ma.record_id = lr.id " +
            "LEFT JOIN knowledge_point kp ON kp.id = ma.kp_id " +
            "WHERE lr.user_id = #{userId} AND lr.is_correct = 0 " +
            "ORDER BY lr.created_at DESC")
    List<Map<String, Object>> selectMistakesByUserId(@Param("userId") Long userId);

    @Select("SELECT lr.id, lr.user_id, lr.question_id, lr.user_answer, lr.is_correct, qb.correct_answer " +
            "FROM learning_record lr LEFT JOIN question_bank qb ON lr.question_id = qb.id " +
            "WHERE lr.id = #{id}")
    Map<String, Object> selectMistakeDetailById(@Param("id") Long id);

    @Update("UPDATE learning_record SET user_answer = #{userAnswer}, is_correct = #{isCorrect} " +
            "WHERE id = #{id} AND user_id = #{userId}")
    int updateRedoResult(@Param("id") Long id,
                         @Param("userId") Long userId,
                         @Param("userAnswer") String userAnswer,
                         @Param("isCorrect") Integer isCorrect);
}
