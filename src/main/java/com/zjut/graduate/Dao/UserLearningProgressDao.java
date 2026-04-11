package com.zjut.graduate.Dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserLearningProgressDao {

    @Update("UPDATE user_learning_progress SET total_seconds = total_seconds + #{delta}, " +
            "last_learned_at = NOW(), updated_at = NOW() WHERE user_id = #{userId} AND section_id = #{sectionId}")
    int addSeconds(@Param("userId") Long userId,
                   @Param("sectionId") Long sectionId,
                   @Param("delta") int delta);

    @Insert("INSERT INTO user_learning_progress " +
            "(user_id, section_id, progress_percent, total_seconds, completed, last_learned_at, created_at, updated_at) " +
            "VALUES (#{userId}, #{sectionId}, 0, #{delta}, 0, NOW(), NOW(), NOW())")
    int insertInitial(@Param("userId") Long userId,
                      @Param("sectionId") Long sectionId,
                      @Param("delta") int delta);
}
