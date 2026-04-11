package com.zjut.graduate.Controller;

import com.zjut.graduate.Po.Course;
import com.zjut.graduate.Po.CourseSection;
import com.zjut.graduate.Po.UserLearningChapterNoteRow;
import com.zjut.graduate.Po.UserLearningNoteRow;
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

    @GetMapping("/courses/{courseId}/outline")
    public Map<String, Object> getCourseOutline(@PathVariable("courseId") Long courseId, HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        Long userId = (Long) session.getAttribute("userId");
        Map<String, Object> data = learningContentService.getCourseLearningOutline(courseId, userId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    @GetMapping("/courses/{courseId}/chapters/{chapterId}/resources")
    public Map<String, Object> listChapterResources(@PathVariable("courseId") Long courseId,
                                                    @PathVariable("chapterId") Long chapterId,
                                                    HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        List<Map<String, Object>> data = learningContentService.listChapterResources(courseId, chapterId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    @GetMapping("/courses/{courseId}/sections")
    public Map<String, Object> listSections(@PathVariable("courseId") Long courseId, HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        Long userId = (Long) session.getAttribute("userId");
        List<CourseSection> data = learningContentService.listSections(courseId, userId);
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
        Long sessionId = learningContentService.startLearningSession(userId, sectionId, deviceInfo);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "学习会话已开始");
        response.put("sessionId", sessionId);
        return response;
    }

    @PostMapping("/sessions/{sessionId}/heartbeat")
    public Map<String, Object> heartbeat(@PathVariable("sessionId") Long sessionId,
                                         @RequestBody Map<String, Object> payload,
                                         HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        Long userId = (Long) session.getAttribute("userId");
        int delta = parsePositiveInt(payload == null ? null : payload.get("deltaSeconds"));
        learningContentService.heartbeatLearningSession(userId, sessionId, delta);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    @PostMapping("/sessions/{sessionId}/end")
    public Map<String, Object> endSession(@PathVariable("sessionId") Long sessionId,
                                          @RequestBody(required = false) Map<String, Object> payload,
                                          HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        Long userId = (Long) session.getAttribute("userId");
        int delta = parsePositiveInt(payload == null ? null : payload.get("finalDeltaSeconds"));
        learningContentService.endLearningSession(userId, sessionId, delta);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    @PostMapping("/sections/{sectionId}/notes")
    public Map<String, Object> addNote(@PathVariable("sectionId") Long sectionId,
                                       @RequestBody Map<String, Object> payload,
                                       HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        Long userId = (Long) session.getAttribute("userId");
        String content = payload == null ? null : String.valueOf(payload.getOrDefault("content", ""));
        Integer timeSec = null;
        if (payload != null && payload.get("timeSec") != null) {
            timeSec = parsePositiveInt(payload.get("timeSec"));
        }
        learningContentService.addLearningNote(userId, sectionId, content, timeSec);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    @GetMapping("/courses/{courseId}/chapters/{chapterId}/notes")
    public Map<String, Object> listChapterNotes(@PathVariable("courseId") Long courseId,
                                                @PathVariable("chapterId") Long chapterId,
                                                @RequestParam(value = "limit", required = false) Integer limit,
                                                HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        Long userId = (Long) session.getAttribute("userId");
        int lim = limit == null ? 50 : limit;
        List<UserLearningChapterNoteRow> data = learningContentService.listChapterLearningNotes(userId, courseId, chapterId, lim);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    @GetMapping("/sections/{sectionId}/notes")
    public Map<String, Object> listNotes(@PathVariable("sectionId") Long sectionId,
                                         @RequestParam(value = "limit", required = false) Integer limit,
                                         HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        Long userId = (Long) session.getAttribute("userId");
        int lim = limit == null ? 20 : limit;
        List<UserLearningNoteRow> data = learningContentService.listLearningNotes(userId, sectionId, lim);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    @PutMapping("/notes/{noteId}")
    public Map<String, Object> updateNote(@PathVariable("noteId") Long noteId,
                                          @RequestBody Map<String, Object> payload,
                                          HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        Long userId = (Long) session.getAttribute("userId");
        String content = payload == null ? null : String.valueOf(payload.getOrDefault("content", ""));
        Integer timeSec = null;
        if (payload != null && payload.get("timeSec") != null) {
            timeSec = parsePositiveInt(payload.get("timeSec"));
        }
        boolean ok = learningContentService.updateLearningNote(userId, noteId, content, timeSec);
        if (!ok) {
            return noteMutationError("更新失败：笔记不存在或内容为空");
        }
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    @DeleteMapping("/notes/{noteId}")
    public Map<String, Object> deleteNote(@PathVariable("noteId") Long noteId, HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        Long userId = (Long) session.getAttribute("userId");
        boolean ok = learningContentService.deleteLearningNote(userId, noteId);
        if (!ok) {
            return noteMutationError("删除失败：笔记不存在");
        }
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    private Map<String, Object> noteMutationError(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
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

    private int parsePositiveInt(Object raw) {
        if (raw == null) {
            return 0;
        }
        if (raw instanceof Number) {
            int v = ((Number) raw).intValue();
            return Math.max(0, v);
        }
        try {
            int v = Integer.parseInt(String.valueOf(raw));
            return Math.max(0, v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

