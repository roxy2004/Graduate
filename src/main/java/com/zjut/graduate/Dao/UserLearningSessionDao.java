package com.zjut.graduate.Dao;

import com.zjut.graduate.Po.UserLearningSession;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserLearningSessionDao {

    @Insert("INSERT INTO user_learning_session (user_id, section_id, start_at, end_at, duration_sec, device_info, created_at) " +
            "VALUES (#{userId}, #{sectionId}, NOW(), NULL, 0, #{deviceInfo}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertStartSession(UserLearningSession session);

    @Update("UPDATE user_learning_session SET duration_sec = duration_sec + #{delta} WHERE id = #{sessionId} AND user_id = #{userId}")
    int addDuration(@Param("userId") Long userId,
                    @Param("sessionId") Long sessionId,
                    @Param("delta") int delta);

    @Update("UPDATE user_learning_session SET end_at = NOW() WHERE id = #{sessionId} AND user_id = #{userId}")
    int markEnded(@Param("userId") Long userId, @Param("sessionId") Long sessionId);

    @Select("SELECT section_id FROM user_learning_session WHERE id = #{sessionId} AND user_id = #{userId} LIMIT 1")
    Long findSectionId(@Param("userId") Long userId, @Param("sessionId") Long sessionId);

    @Select("SELECT IFNULL(SUM(duration_sec), 0) FROM user_learning_session WHERE user_id = #{userId} AND section_id = #{sectionId}")
    int sumDurationByUserSection(@Param("userId") Long userId, @Param("sectionId") Long sectionId);
}

