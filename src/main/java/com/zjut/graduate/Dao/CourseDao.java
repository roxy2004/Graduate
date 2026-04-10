package com.zjut.graduate.Dao;

import com.zjut.graduate.Po.Course;
import com.zjut.graduate.Po.CourseSection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseDao {

    @Select("SELECT id, name AS title, description, NULL AS cover_url, NULL AS level, " +
            "is_active AS status, created_at, updated_at " +
            "FROM course WHERE is_active = 1 ORDER BY created_at DESC")
    List<Course> selectActiveCourses();

    @Select("SELECT s.id, s.course_id AS chapter_id, s.title, 'video' AS section_type, " +
            "(s.estimated_minutes * 60) AS duration_seconds, s.sort_no, c.name AS chapter_title " +
            "FROM course_section s " +
            "INNER JOIN course c ON s.course_id = c.id " +
            "WHERE s.course_id = #{courseId} AND s.is_active = 1 " +
            "ORDER BY s.sort_no ASC")
    List<CourseSection> selectSectionsByCourseId(Long courseId);
}

