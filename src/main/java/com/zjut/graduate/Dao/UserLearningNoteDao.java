package com.zjut.graduate.Dao;

import com.zjut.graduate.Po.UserLearningChapterNoteRow;
import com.zjut.graduate.Po.UserLearningNoteRow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserLearningNoteDao {

    @Insert("INSERT INTO user_learning_note (user_id, section_id, note_content, note_time_sec, created_at, updated_at) " +
            "VALUES (#{userId}, #{sectionId}, #{content}, #{timeSec}, NOW(), NOW())")
    int insert(@Param("userId") Long userId,
               @Param("sectionId") Long sectionId,
               @Param("content") String content,
               @Param("timeSec") int timeSec);

    @Select("SELECT COUNT(1) FROM user_learning_note WHERE user_id = #{userId} AND section_id = #{sectionId}")
    int countByUserAndSection(@Param("userId") Long userId, @Param("sectionId") Long sectionId);

    @Select("SELECT id, note_content AS noteContent, note_time_sec AS noteTimeSec, created_at AS createdAt " +
            "FROM user_learning_note WHERE user_id = #{userId} AND section_id = #{sectionId} " +
            "ORDER BY created_at DESC, id DESC LIMIT #{limit}")
    List<UserLearningNoteRow> listRecentByUserSection(@Param("userId") Long userId,
                                                      @Param("sectionId") Long sectionId,
                                                      @Param("limit") int limit);

    /**
     * 按课程章节聚合笔记（依赖 course_section.chapter_id）。
     */
    @Select("SELECT n.id, n.section_id AS sectionId, s.title AS sectionTitle, " +
            "n.note_content AS noteContent, n.note_time_sec AS noteTimeSec, n.created_at AS createdAt " +
            "FROM user_learning_note n " +
            "INNER JOIN course_section s ON s.id = n.section_id AND s.course_id = #{courseId} AND s.is_active = 1 " +
            "WHERE n.user_id = #{userId} AND s.chapter_id = #{chapterId} " +
            "ORDER BY n.created_at DESC, n.id DESC " +
            "LIMIT #{limit}")
    List<UserLearningChapterNoteRow> listRecentByUserCourseChapter(@Param("userId") Long userId,
                                                                   @Param("courseId") Long courseId,
                                                                   @Param("chapterId") Long chapterId,
                                                                   @Param("limit") int limit);

    @Update("UPDATE user_learning_note SET note_content = #{content}, note_time_sec = #{timeSec}, updated_at = NOW() " +
            "WHERE id = #{noteId} AND user_id = #{userId}")
    int updateByUser(@Param("userId") Long userId,
                     @Param("noteId") Long noteId,
                     @Param("content") String content,
                     @Param("timeSec") int timeSec);

    @Delete("DELETE FROM user_learning_note WHERE id = #{noteId} AND user_id = #{userId}")
    int deleteByUser(@Param("userId") Long userId, @Param("noteId") Long noteId);
}
