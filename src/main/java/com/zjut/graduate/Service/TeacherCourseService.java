package com.zjut.graduate.Service;

import com.zjut.graduate.Po.Course;
import com.zjut.graduate.Po.CourseChapter;
import com.zjut.graduate.Po.CourseSection;

import java.util.List;
import java.util.Map;

public interface TeacherCourseService {

    List<Course> listActiveCourses();

    List<CourseChapter> listChapters(Long courseId);

    CourseChapter addChapter(Long courseId, String title, Integer sortNo);

    void updateChapter(Long chapterId, String title, Integer sortNo);

    void deleteChapter(Long chapterId);

    List<Map<String, Object>> listChapterResources(Long chapterId);

    void addChapterResource(Long chapterId, String resourceType, String title, String url);

    void deleteResource(Long resourceId);

    List<CourseSection> listSections(Long courseId, Long chapterId);

    CourseSection addSection(Long courseId, Long chapterId, String title, Integer sortNo, Integer estimatedMinutes);

    void updateSection(Long sectionId, String title, Integer sortNo, Integer estimatedMinutes);

    void deleteSection(Long sectionId);

    List<Map<String, Object>> listSectionResources(Long sectionId);

    void addSectionResource(Long sectionId, String resourceType, String title, String url);
}
