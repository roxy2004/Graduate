package com.zjut.graduate.Dao;

import com.zjut.graduate.Po.CourseChapter;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CourseChapterDao {

    @Select("SELECT id, course_id AS courseId, title, sort_no AS sortNo FROM course_chapter " +
            "WHERE course_id = #{courseId} ORDER BY sort_no ASC, id ASC")
    List<CourseChapter> listByCourseId(@Param("courseId") Long courseId);

    @Select("SELECT id, course_id AS courseId, title, sort_no AS sortNo FROM course_chapter WHERE id = #{id} LIMIT 1")
    CourseChapter selectById(@Param("id") Long id);

    @Insert("INSERT INTO course_chapter (course_id, title, sort_no) VALUES (#{courseId}, #{title}, #{sortNo})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CourseChapter row);

    @Update("UPDATE course_chapter SET title = #{title}, sort_no = #{sortNo} WHERE id = #{id}")
    int update(CourseChapter row);

    @Delete("DELETE FROM course_chapter WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
