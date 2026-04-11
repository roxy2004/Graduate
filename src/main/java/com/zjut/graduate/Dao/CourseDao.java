package com.zjut.graduate.Dao;

import com.zjut.graduate.Po.Course;
import com.zjut.graduate.Po.CourseSection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseDao {

    @Select("SELECT id, name AS title, description, NULL AS cover_url, NULL AS level, " +
            "is_active AS status, created_at, updated_at " +
            "FROM course WHERE is_active = 1 ORDER BY created_at DESC")
    List<Course> selectActiveCourses();

    /**
     * 小节按 course_section.chapter_id 归属章节（需已回填/维护 chapter_id）。
     */
    @Select("SELECT s.id, s.chapter_id AS chapter_id, ch.sort_no AS chapter_sort_no, " +
            "s.title, 'video' AS section_type, " +
            "(s.estimated_minutes * 60) AS duration_seconds, s.sort_no, ch.title AS chapter_title, " +
            "IFNULL(st.learned_seconds, 0) AS learned_seconds, IFNULL(nc.note_count, 0) AS note_count " +
            "FROM course_section s " +
            "INNER JOIN course c ON s.course_id = c.id " +
            "INNER JOIN course_chapter ch ON ch.id = s.chapter_id AND ch.course_id = c.id " +
            "LEFT JOIN (" +
            "  SELECT section_id, IFNULL(SUM(duration_sec), 0) AS learned_seconds " +
            "  FROM user_learning_session WHERE user_id = #{userId} GROUP BY section_id" +
            ") st ON st.section_id = s.id " +
            "LEFT JOIN (" +
            "  SELECT section_id, COUNT(1) AS note_count " +
            "  FROM user_learning_note WHERE user_id = #{userId} GROUP BY section_id" +
            ") nc ON nc.section_id = s.id " +
            "WHERE s.course_id = #{courseId} AND s.is_active = 1 " +
            "ORDER BY ch.sort_no ASC, s.sort_no ASC")
    List<CourseSection> selectSectionsByCourseId(@Param("courseId") Long courseId, @Param("userId") Long userId);
}
