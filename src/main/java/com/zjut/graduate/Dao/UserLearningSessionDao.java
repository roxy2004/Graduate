package com.zjut.graduate.Dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserLearningSessionDao {

    @Insert("INSERT INTO user_learning_session (user_id, section_id, start_at, end_at, duration_sec, device_info, created_at) " +
            "VALUES (#{userId}, #{sectionId}, NOW(), NULL, 0, #{deviceInfo}, NOW())")
    int insertStartSession(@Param("userId") Long userId,
                           @Param("sectionId") Long sectionId,
                           @Param("deviceInfo") String deviceInfo);
}

