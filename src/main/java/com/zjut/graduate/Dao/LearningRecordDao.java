package com.zjut.graduate.Dao;

import com.zjut.graduate.Po.LearningRecord;
import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;
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

    @Insert("INSERT INTO learning_record (user_id, question_id, user_answer, is_correct, time_spent, created_at) " +
            "VALUES (#{userId}, #{questionId}, #{userAnswer}, #{isCorrect}, #{timeSpent}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LearningRecord record);
}
