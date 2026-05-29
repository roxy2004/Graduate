package com.zjut.graduate.Service;

import com.zjut.graduate.Po.Course;
import com.zjut.graduate.Po.CourseSection;
import com.zjut.graduate.Po.UserLearningChapterNoteRow;
import com.zjut.graduate.Po.UserLearningNoteRow;

import java.util.List;
import java.util.Map;

/** 学生专项学习内容、会话与笔记 */
public interface LearningContentService {

    /** 查询可选课程列表 */
    List<Course> listCourses();

    /** 查询课程下小节列表（含学习进度） */
    List<CourseSection> listSections(Long courseId, Long userId);

    /** 课程学习大纲：章节、小节与合并资源 */
    Map<String, Object> getCourseLearningOutline(Long courseId, Long userId);

    /** 查询小节学习资源 */
    List<Map<String, Object>> listResources(Long sectionId);

    /** 查询章节级外链资源 */
    List<Map<String, Object>> listChapterResources(Long courseId, Long chapterId);

    /** 开始小节学习会话，返回 sessionId */
    Long startLearningSession(Long userId, Long sectionId, String deviceInfo);

    /** 学习会话心跳，累加时长 */
    void heartbeatLearningSession(Long userId, Long sessionId, int deltaSeconds);

    /** 结束学习会话 */
    void endLearningSession(Long userId, Long sessionId, int finalDeltaSeconds);

    /** 新增学习笔记 */
    void addLearningNote(Long userId, Long sectionId, String content, Integer timeSec);

    /** 查询小节笔记列表 */
    List<UserLearningNoteRow> listLearningNotes(Long userId, Long sectionId, int limit);

    /** 按章节聚合查询笔记 */
    List<UserLearningChapterNoteRow> listChapterLearningNotes(Long userId, Long courseId, Long chapterId, int limit);

    /** 更新笔记，成功表示笔记存在且属于该用户 */
    boolean updateLearningNote(Long userId, Long noteId, String content, Integer timeSec);

    /** 删除笔记，成功表示笔记存在且属于该用户 */
    boolean deleteLearningNote(Long userId, Long noteId);
}
