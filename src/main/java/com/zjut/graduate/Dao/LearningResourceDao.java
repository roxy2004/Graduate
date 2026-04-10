package com.zjut.graduate.Dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface LearningResourceDao {

    @Select("SELECT id, section_id, resource_type, title, url AS resource_url, duration_sec AS duration_seconds " +
            "FROM learning_resource WHERE section_id = #{sectionId} ORDER BY id ASC")
    List<Map<String, Object>> selectBySectionId(Long sectionId);
}

