package com.zjut.graduate.Service;

import com.zjut.graduate.Po.Course;
import com.zjut.graduate.Po.CourseSection;

import java.util.List;
import java.util.Map;

public interface LearningContentService {
    List<Course> listCourses();

    List<CourseSection> listSections(Long courseId);

    List<Map<String, Object>> listResources(Long sectionId);

    void startLearningSession(Long userId, Long sectionId, String deviceInfo);
}

