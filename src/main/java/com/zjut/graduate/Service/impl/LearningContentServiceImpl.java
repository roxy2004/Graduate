package com.zjut.graduate.Service.impl;

import com.zjut.graduate.Dao.CourseDao;
import com.zjut.graduate.Dao.LearningResourceDao;
import com.zjut.graduate.Dao.UserLearningSessionDao;
import com.zjut.graduate.Po.Course;
import com.zjut.graduate.Po.CourseSection;
import com.zjut.graduate.Service.LearningContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LearningContentServiceImpl implements LearningContentService {

    @Autowired
    private CourseDao courseDao;

    @Autowired
    private LearningResourceDao learningResourceDao;

    @Autowired
    private UserLearningSessionDao userLearningSessionDao;

    @Override
    public List<Course> listCourses() {
        return courseDao.selectActiveCourses();
    }

    @Override
    public List<CourseSection> listSections(Long courseId) {
        return courseDao.selectSectionsByCourseId(courseId);
    }

    @Override
    public List<Map<String, Object>> listResources(Long sectionId) {
        return learningResourceDao.selectBySectionId(sectionId);
    }

    @Override
    public void startLearningSession(Long userId, Long sectionId, String deviceInfo) {
        userLearningSessionDao.insertStartSession(userId, sectionId, deviceInfo);
    }
}

