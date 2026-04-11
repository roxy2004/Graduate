package com.zjut.graduate.Service.impl;

import com.zjut.graduate.Dao.CourseChapterDao;
import com.zjut.graduate.Dao.CourseDao;
import com.zjut.graduate.Dao.CourseSectionDao;
import com.zjut.graduate.Dao.LearningResourceDao;
import com.zjut.graduate.Po.Course;
import com.zjut.graduate.Po.CourseChapter;
import com.zjut.graduate.Po.CourseSection;
import com.zjut.graduate.Service.TeacherCourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class TeacherCourseServiceImpl implements TeacherCourseService {

    @Autowired
    private CourseDao courseDao;

    @Autowired
    private CourseChapterDao courseChapterDao;

    @Autowired
    private LearningResourceDao learningResourceDao;

    @Autowired
    private CourseSectionDao courseSectionDao;

    @Override
    public List<Course> listActiveCourses() {
        return courseDao.selectActiveCourses();
    }

    @Override
    public List<CourseChapter> listChapters(Long courseId) {
        return courseChapterDao.listByCourseId(courseId);
    }

    @Override
    @Transactional
    public CourseChapter addChapter(Long courseId, String title, Integer sortNo) {
        CourseChapter row = new CourseChapter();
        row.setCourseId(courseId);
        row.setTitle(title == null ? "" : title.trim());
        row.setSortNo(sortNo == null ? 0 : sortNo);
        courseChapterDao.insert(row);
        return courseChapterDao.selectById(row.getId());
    }

    @Override
    @Transactional
    public void updateChapter(Long chapterId, String title, Integer sortNo) {
        CourseChapter existing = courseChapterDao.selectById(chapterId);
        if (existing == null) {
            return;
        }
        CourseChapter row = new CourseChapter();
        row.setId(chapterId);
        row.setCourseId(existing.getCourseId());
        row.setTitle(title == null ? existing.getTitle() : title.trim());
        row.setSortNo(sortNo == null ? existing.getSortNo() : sortNo);
        courseChapterDao.update(row);
    }

    @Override
    @Transactional
    public void deleteChapter(Long chapterId) {
        learningResourceDao.deleteByChapterId(chapterId);
        courseChapterDao.deleteById(chapterId);
    }

    @Override
    public List<Map<String, Object>> listChapterResources(Long chapterId) {
        CourseChapter ch = courseChapterDao.selectById(chapterId);
        if (ch == null) {
            return java.util.Collections.emptyList();
        }
        return learningResourceDao.selectByCourseAndChapterId(ch.getCourseId(), chapterId);
    }

    @Override
    @Transactional
    public void addChapterResource(Long chapterId, String resourceType, String title, String url) {
        CourseChapter ch = courseChapterDao.selectById(chapterId);
        if (ch == null) {
            return;
        }
        String type = resourceType == null ? "doc" : resourceType.trim().toLowerCase();
        if (!"video".equals(type) && !"doc".equals(type)) {
            type = "doc";
        }
        learningResourceDao.insertChapterResource(chapterId, type,
                title == null ? "" : title.trim(),
                url == null ? "" : url.trim());
    }

    @Override
    @Transactional
    public void deleteResource(Long resourceId) {
        learningResourceDao.deleteById(resourceId);
    }

    @Override
    public List<CourseSection> listSections(Long courseId, Long chapterId) {
        CourseChapter ch = courseChapterDao.selectById(chapterId);
        if (ch == null || ch.getCourseId() == null || !ch.getCourseId().equals(courseId)) {
            return java.util.Collections.emptyList();
        }
        return courseSectionDao.listActiveByChapter(courseId, chapterId);
    }

    @Override
    @Transactional
    public CourseSection addSection(Long courseId, Long chapterId, String title, Integer sortNo, Integer estimatedMinutes) {
        CourseChapter ch = courseChapterDao.selectById(chapterId);
        if (ch == null || ch.getCourseId() == null || !ch.getCourseId().equals(courseId)) {
            return null;
        }
        CourseSection row = new CourseSection();
        row.setCourseId(courseId);
        row.setChapterId(chapterId);
        row.setTitle(title == null ? "" : title.trim());
        row.setSortNo(sortNo == null ? 0 : sortNo);
        int em = estimatedMinutes == null ? 10 : Math.max(1, estimatedMinutes);
        row.setEstimatedMinutes(em);
        courseSectionDao.insert(row);
        return courseSectionDao.selectActiveById(row.getId());
    }

    @Override
    @Transactional
    public void updateSection(Long sectionId, String title, Integer sortNo, Integer estimatedMinutes) {
        CourseSection existing = courseSectionDao.selectActiveById(sectionId);
        if (existing == null || existing.getCourseId() == null) {
            return;
        }
        int em = estimatedMinutes == null
                ? (existing.getEstimatedMinutes() == null ? 10 : existing.getEstimatedMinutes())
                : Math.max(1, estimatedMinutes);
        String t = title == null ? existing.getTitle() : title.trim();
        if (t.isEmpty()) {
            t = existing.getTitle();
        }
        courseSectionDao.updateActive(sectionId, existing.getCourseId(), t,
                sortNo == null ? existing.getSortNo() : sortNo,
                em);
    }

    @Override
    @Transactional
    public void deleteSection(Long sectionId) {
        CourseSection existing = courseSectionDao.selectActiveById(sectionId);
        if (existing == null || existing.getCourseId() == null) {
            return;
        }
        learningResourceDao.deleteBySectionId(sectionId);
        courseSectionDao.softDelete(sectionId, existing.getCourseId());
    }

    @Override
    public List<Map<String, Object>> listSectionResources(Long sectionId) {
        CourseSection s = courseSectionDao.selectActiveById(sectionId);
        if (s == null) {
            return java.util.Collections.emptyList();
        }
        return learningResourceDao.selectBySectionId(sectionId);
    }

    @Override
    @Transactional
    public void addSectionResource(Long sectionId, String resourceType, String title, String url) {
        CourseSection s = courseSectionDao.selectActiveById(sectionId);
        if (s == null) {
            return;
        }
        String type = resourceType == null ? "doc" : resourceType.trim().toLowerCase();
        if (!"video".equals(type) && !"doc".equals(type)) {
            type = "doc";
        }
        learningResourceDao.insertSectionResource(sectionId, type,
                title == null ? "" : title.trim(),
                url == null ? "" : url.trim());
    }
}
