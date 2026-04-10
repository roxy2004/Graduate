package com.zjut.graduate.Controller;

import com.zjut.graduate.Po.Course;
import com.zjut.graduate.Po.CourseSection;
import com.zjut.graduate.Service.LearningContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/xwd/student/learning")
public class StudentLearningController {

    @Autowired
    private LearningContentService learningContentService;

    @GetMapping("/courses")
    public Map<String, Object> listCourses(HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        List<Course> data = learningContentService.listCourses();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    @GetMapping("/courses/{courseId}/sections")
    public Map<String, Object> listSections(@PathVariable("courseId") Long courseId, HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        List<CourseSection> data = learningContentService.listSections(courseId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    @GetMapping("/sections/{sectionId}/resources")
    public Map<String, Object> listResources(@PathVariable("sectionId") Long sectionId, HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        List<Map<String, Object>> data = learningContentService.listResources(sectionId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    @PostMapping("/sections/{sectionId}/start")
    public Map<String, Object> startLearning(@PathVariable("sectionId") Long sectionId, HttpSession session,
                                             @RequestBody(required = false) Map<String, String> payload) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        Long userId = (Long) session.getAttribute("userId");
        String deviceInfo = payload == null ? "web" : payload.getOrDefault("deviceInfo", "web");
        learningContentService.startLearningSession(userId, sectionId, deviceInfo);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "学习会话已开始");
        return response;
    }

    private Map<String, Object> requireStudent(HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"student".equals(role)) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("code", 403);
            response.put("message", "仅学生可操作");
            return response;
        }
        return null;
    }
}

