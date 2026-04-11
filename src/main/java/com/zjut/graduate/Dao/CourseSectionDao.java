package com.zjut.graduate.Dao;

import com.zjut.graduate.Po.CourseSection;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CourseSectionDao {

    @Select("SELECT s.id, s.course_id AS courseId, s.chapter_id AS chapterId, ch.sort_no AS chapterSortNo, " +
            "s.title, 'video' AS sectionType, (s.estimated_minutes * 60) AS durationSeconds, s.sort_no, " +
            "s.estimated_minutes AS estimatedMinutes, ch.title AS chapterTitle, 0 AS learnedSeconds, 0 AS noteCount " +
            "FROM course_section s " +
            "INNER JOIN course_chapter ch ON ch.id = s.chapter_id AND ch.course_id = #{courseId} " +
            "WHERE s.chapter_id = #{chapterId} AND s.is_active = 1 " +
            "ORDER BY s.sort_no ASC, s.id ASC")
    List<CourseSection> listActiveByChapter(@Param("courseId") Long courseId, @Param("chapterId") Long chapterId);

    @Select("SELECT s.id, s.course_id AS courseId, s.chapter_id AS chapterId, s.title, s.sort_no AS sortNo, " +
            "s.estimated_minutes AS estimatedMinutes FROM course_section s " +
            "WHERE s.id = #{id} AND s.is_active = 1 LIMIT 1")
    CourseSection selectActiveById(@Param("id") Long id);

    @Insert("INSERT INTO course_section (course_id, chapter_id, title, sort_no, estimated_minutes, is_active) " +
            "VALUES (#{courseId}, #{chapterId}, #{title}, #{sortNo}, #{estimatedMinutes}, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CourseSection row);

    @Update("UPDATE course_section SET title = #{title}, sort_no = #{sortNo}, " +
            "estimated_minutes = #{estimatedMinutes} WHERE id = #{id} AND course_id = #{courseId} AND is_active = 1")
    int updateActive(@Param("id") Long id,
                     @Param("courseId") Long courseId,
                     @Param("title") String title,
                     @Param("sortNo") Integer sortNo,
                     @Param("estimatedMinutes") Integer estimatedMinutes);

    @Update("UPDATE course_section SET is_active = 0 WHERE id = #{id} AND course_id = #{courseId} AND is_active = 1")
    int softDelete(@Param("id") Long id, @Param("courseId") Long courseId);
}
