package com.zjut.graduate.Service;

import com.zjut.graduate.Po.Course;
import com.zjut.graduate.Po.CourseSection;
import com.zjut.graduate.Po.UserLearningChapterNoteRow;
import com.zjut.graduate.Po.UserLearningNoteRow;

import java.util.List;
import java.util.Map;

public interface LearningContentService {
    List<Course> listCourses();

    List<CourseSection> listSections(Long courseId, Long userId);

    /**
     * 课程学习大纲：数据库中的全部章节 + 每章下的小节与合并后的外链资源（章节直链 + 小节资源）。
     */
    Map<String, Object> getCourseLearningOutline(Long courseId, Long userId);

    List<Map<String, Object>> listResources(Long sectionId);

    /** 章节级外链资源（视频/文档），供学生端展示 */
    List<Map<String, Object>> listChapterResources(Long courseId, Long chapterId);

    Long startLearningSession(Long userId, Long sectionId, String deviceInfo);

    void heartbeatLearningSession(Long userId, Long sessionId, int deltaSeconds);

    void endLearningSession(Long userId, Long sessionId, int finalDeltaSeconds);

    void addLearningNote(Long userId, Long sectionId, String content, Integer timeSec);

    List<UserLearningNoteRow> listLearningNotes(Long userId, Long sectionId, int limit);

    List<UserLearningChapterNoteRow> listChapterLearningNotes(Long userId, Long courseId, Long chapterId, int limit);

    /** @return 是否更新成功（笔记存在且属于该用户） */
    boolean updateLearningNote(Long userId, Long noteId, String content, Integer timeSec);

    /** @return 是否删除成功（笔记存在且属于该用户） */
    boolean deleteLearningNote(Long userId, Long noteId);
}

