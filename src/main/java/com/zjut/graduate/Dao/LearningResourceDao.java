package com.zjut.graduate.Dao;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface LearningResourceDao {

    @Select("SELECT id, section_id, resource_type, title, url AS resource_url, IFNULL(duration_sec, 0) AS duration_seconds " +
            "FROM learning_resource WHERE section_id = #{sectionId} ORDER BY id ASC")
    List<Map<String, Object>> selectBySectionId(Long sectionId);

    /**
     * 章节下资源：章节直链 + 挂在属于该章节的小节上的资源（原专项学习数据多为 section_id）。
     */
    @Select("SELECT id, section_id, chapter_id, resource_type, title, resource_url, duration_seconds, resource_scope FROM (" +
            "  SELECT lr.id, lr.section_id, lr.chapter_id, lr.resource_type, lr.title, lr.url AS resource_url, " +
            "         IFNULL(lr.duration_sec, 0) AS duration_seconds, 'chapter' AS resource_scope " +
            "  FROM learning_resource lr " +
            "  WHERE lr.chapter_id = #{chapterId} " +
            "  UNION " +
            "  SELECT lr.id, lr.section_id, lr.chapter_id, lr.resource_type, lr.title, lr.url AS resource_url, " +
            "         IFNULL(lr.duration_sec, 0) AS duration_seconds, 'section' AS resource_scope " +
            "  FROM learning_resource lr " +
            "  INNER JOIN course_section s ON s.id = lr.section_id AND s.course_id = #{courseId} AND s.chapter_id = #{chapterId} " +
            "  WHERE lr.section_id IS NOT NULL " +
            ") t ORDER BY t.id ASC")
    List<Map<String, Object>> selectByCourseAndChapterId(@Param("courseId") Long courseId, @Param("chapterId") Long chapterId);

    /**
     * 课程下全部资源并计算归属章节（COALESCE 章节直链、小节所属章）。
     */
    @Select("SELECT lr.id, lr.section_id, lr.chapter_id, lr.resource_type, lr.title, lr.url AS resource_url, " +
            "IFNULL(lr.duration_sec, 0) AS duration_seconds, " +
            "IFNULL(lr.chapter_id, s.chapter_id) AS owner_chapter_id " +
            "FROM learning_resource lr " +
            "LEFT JOIN course_section s ON s.id = lr.section_id " +
            "WHERE lr.chapter_id IN (SELECT id FROM course_chapter WHERE course_id = #{courseId}) " +
            "   OR lr.section_id IN (SELECT id FROM course_section WHERE course_id = #{courseId} AND is_active = 1) " +
            "ORDER BY lr.id ASC")
    List<Map<String, Object>> selectResourcesWithOwnerChapterForCourse(@Param("courseId") Long courseId);

    @Insert("INSERT INTO learning_resource (chapter_id, section_id, resource_type, title, url, duration_sec) " +
            "VALUES (#{chapterId}, NULL, #{resourceType}, #{title}, #{url}, 0)")
    int insertChapterResource(@Param("chapterId") Long chapterId,
                              @Param("resourceType") String resourceType,
                              @Param("title") String title,
                              @Param("url") String url);

    @Insert("INSERT INTO learning_resource (chapter_id, section_id, resource_type, title, url, duration_sec) " +
            "VALUES (NULL, #{sectionId}, #{resourceType}, #{title}, #{url}, 0)")
    int insertSectionResource(@Param("sectionId") Long sectionId,
                              @Param("resourceType") String resourceType,
                              @Param("title") String title,
                              @Param("url") String url);

    @Delete("DELETE FROM learning_resource WHERE section_id = #{sectionId}")
    int deleteBySectionId(@Param("sectionId") Long sectionId);

    @Delete("DELETE FROM learning_resource WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Delete("DELETE FROM learning_resource WHERE chapter_id = #{chapterId}")
    int deleteByChapterId(@Param("chapterId") Long chapterId);
}
