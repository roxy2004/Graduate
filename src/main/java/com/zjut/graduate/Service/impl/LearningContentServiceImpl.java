package com.zjut.graduate.Service.impl;

import com.zjut.graduate.Dao.CourseChapterDao;
import com.zjut.graduate.Dao.CourseDao;
import com.zjut.graduate.Dao.LearningResourceDao;
import com.zjut.graduate.Dao.UserLearningNoteDao;
import com.zjut.graduate.Dao.UserLearningProgressDao;
import com.zjut.graduate.Dao.UserLearningSessionDao;
import com.zjut.graduate.Po.Course;
import com.zjut.graduate.Po.CourseChapter;
import com.zjut.graduate.Po.CourseSection;
import com.zjut.graduate.Po.UserLearningChapterNoteRow;
import com.zjut.graduate.Po.UserLearningNoteRow;
import com.zjut.graduate.Po.UserLearningSession;
import com.zjut.graduate.Service.LearningContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LearningContentServiceImpl implements LearningContentService {

    @Autowired
    private CourseDao courseDao;

    @Autowired
    private CourseChapterDao courseChapterDao;

    @Autowired
    private LearningResourceDao learningResourceDao;

    @Autowired
    private UserLearningSessionDao userLearningSessionDao;

    @Autowired
    private UserLearningProgressDao userLearningProgressDao;

    @Autowired
    private UserLearningNoteDao userLearningNoteDao;

    @Override
    public List<Course> listCourses() {
        return courseDao.selectActiveCourses();
    }

    @Override
    public List<CourseSection> listSections(Long courseId, Long userId) {
        return courseDao.selectSectionsByCourseId(courseId, userId);
    }

    @Override
    public Map<String, Object> getCourseLearningOutline(Long courseId, Long userId) {
        List<CourseChapter> chapters = courseChapterDao.listByCourseId(courseId);
        List<CourseSection> sections = courseDao.selectSectionsByCourseId(courseId, userId);
        List<Map<String, Object>> rawRes = learningResourceDao.selectResourcesWithOwnerChapterForCourse(courseId);

        Map<Long, List<Map<String, Object>>> resByChapter = new HashMap<>();
        for (Map<String, Object> r : rawRes) {
            Object oc = r.get("owner_chapter_id");
            if (oc == null) {
                continue;
            }
            long cid = ((Number) oc).longValue();
            Map<String, Object> copy = new LinkedHashMap<>(r);
            copy.remove("owner_chapter_id");
            resByChapter.computeIfAbsent(cid, k -> new ArrayList<>()).add(copy);
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (CourseChapter ch : chapters) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", ch.getId());
            row.put("title", ch.getTitle());
            row.put("sortNo", ch.getSortNo());
            List<CourseSection> secs = new ArrayList<>();
            for (CourseSection s : sections) {
                if (ch.getId().equals(s.getChapterId())) {
                    secs.add(s);
                }
            }
            row.put("sections", secs);
            row.put("resources", resByChapter.getOrDefault(ch.getId(), Collections.emptyList()));
            int tl = 0;
            int tn = 0;
            for (CourseSection s : secs) {
                if (s.getLearnedSeconds() != null) {
                    tl += s.getLearnedSeconds();
                }
                if (s.getNoteCount() != null) {
                    tn += s.getNoteCount();
                }
            }
            row.put("totalLearnedSeconds", tl);
            row.put("totalNoteCount", tn);
            out.add(row);
        }
        Map<String, Object> wrap = new LinkedHashMap<>();
        wrap.put("chapters", out);
        return wrap;
    }

    @Override
    public List<Map<String, Object>> listResources(Long sectionId) {
        return learningResourceDao.selectBySectionId(sectionId);
    }

    @Override
    public List<Map<String, Object>> listChapterResources(Long courseId, Long chapterId) {
        return learningResourceDao.selectByCourseAndChapterId(courseId, chapterId);
    }

    @Override
    @Transactional
    public Long startLearningSession(Long userId, Long sectionId, String deviceInfo) {
        UserLearningSession row = new UserLearningSession();
        row.setUserId(userId);
        row.setSectionId(sectionId);
        row.setDeviceInfo(deviceInfo);
        userLearningSessionDao.insertStartSession(row);
        return row.getId();
    }

    @Override
    @Transactional
    public void heartbeatLearningSession(Long userId, Long sessionId, int deltaSeconds) {
        int delta = clampDelta(deltaSeconds);
        if (delta <= 0) {
            return;
        }
        userLearningSessionDao.addDuration(userId, sessionId, delta);
        Long sectionId = userLearningSessionDao.findSectionId(userId, sessionId);
        if (sectionId == null) {
            return;
        }
        applyProgressDelta(userId, sectionId, delta);
    }

    @Override
    @Transactional
    public void endLearningSession(Long userId, Long sessionId, int finalDeltaSeconds) {
        int delta = clampDelta(finalDeltaSeconds);
        Long sectionId = userLearningSessionDao.findSectionId(userId, sessionId);
        if (sectionId != null && delta > 0) {
            userLearningSessionDao.addDuration(userId, sessionId, delta);
            applyProgressDelta(userId, sectionId, delta);
        }
        userLearningSessionDao.markEnded(userId, sessionId);
    }

    @Override
    @Transactional
    public void addLearningNote(Long userId, Long sectionId, String content, Integer timeSec) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }
        int t = timeSec == null ? 0 : Math.max(0, timeSec);
        userLearningNoteDao.insert(userId, sectionId, content.trim(), t);
    }

    @Override
    public List<UserLearningNoteRow> listLearningNotes(Long userId, Long sectionId, int limit) {
        int lim = limit <= 0 ? 20 : Math.min(limit, 50);
        return userLearningNoteDao.listRecentByUserSection(userId, sectionId, lim);
    }

    @Override
    public List<UserLearningChapterNoteRow> listChapterLearningNotes(Long userId, Long courseId, Long chapterId, int limit) {
        int lim = limit <= 0 ? 50 : Math.min(limit, 200);
        return userLearningNoteDao.listRecentByUserCourseChapter(userId, courseId, chapterId, lim);
    }

    @Override
    @Transactional
    public boolean updateLearningNote(Long userId, Long noteId, String content, Integer timeSec) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        int t = timeSec == null ? 0 : Math.max(0, timeSec);
        return userLearningNoteDao.updateByUser(userId, noteId, content.trim(), t) > 0;
    }

    @Override
    @Transactional
    public boolean deleteLearningNote(Long userId, Long noteId) {
        return userLearningNoteDao.deleteByUser(userId, noteId) > 0;
    }

    private void applyProgressDelta(Long userId, Long sectionId, int deltaSeconds) {
        int updated = userLearningProgressDao.addSeconds(userId, sectionId, deltaSeconds);
        if (updated == 0) {
            userLearningProgressDao.insertInitial(userId, sectionId, deltaSeconds);
        }
    }

    private int clampDelta(int deltaSeconds) {
        if (deltaSeconds <= 0) {
            return 0;
        }
        return Math.min(deltaSeconds, 15 * 60);
    }
}

