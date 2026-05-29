package com.zjut.graduate.Service;

import com.zjut.graduate.Po.Course;
import com.zjut.graduate.Po.CourseChapter;
import com.zjut.graduate.Po.CourseSection;

import java.util.List;
import java.util.Map;

/** 教师端课程、章节、小节与资源管理 */
public interface TeacherCourseService {

    /** 查询启用中的课程 */
    List<Course> listActiveCourses();

    /** 查询课程章节列表 */
    List<CourseChapter> listChapters(Long courseId);

    /** 新增章节 */
    CourseChapter addChapter(Long courseId, String title, Integer sortNo);

    /** 更新章节 */
    void updateChapter(Long chapterId, String title, Integer sortNo);

    /** 删除章节 */
    void deleteChapter(Long chapterId);

    /** 查询章节资源列表 */
    List<Map<String, Object>> listChapterResources(Long chapterId);

    /** 新增章节外链资源 */
    void addChapterResource(Long chapterId, String resourceType, String title, String url);

    /** 删除学习资源 */
    void deleteResource(Long resourceId);

    /** 查询章节下小节列表 */
    List<CourseSection> listSections(Long courseId, Long chapterId);

    /** 新增小节 */
    CourseSection addSection(Long courseId, Long chapterId, String title, Integer sortNo, Integer estimatedMinutes);

    /** 更新小节 */
    void updateSection(Long sectionId, String title, Integer sortNo, Integer estimatedMinutes);

    /** 删除小节 */
    void deleteSection(Long sectionId);

    /** 查询小节资源列表 */
    List<Map<String, Object>> listSectionResources(Long sectionId);

    /** 新增小节外链资源 */
    void addSectionResource(Long sectionId, String resourceType, String title, String url);
}
