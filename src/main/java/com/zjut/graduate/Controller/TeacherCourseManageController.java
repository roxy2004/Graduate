package com.zjut.graduate.Controller;

import com.zjut.graduate.Po.Course;
import com.zjut.graduate.Po.CourseChapter;
import com.zjut.graduate.Po.CourseSection;
import com.zjut.graduate.Service.TeacherCourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/xwd/teacher/course-manage")
public class TeacherCourseManageController {

    @Autowired
    private TeacherCourseService teacherCourseService;

    @GetMapping("/courses")
    public Map<String, Object> listCourses(HttpSession session) {
        Map<String, Object> authError = requireTeacher(session);
        if (authError != null) {
            return authError;
        }
        List<Course> data = teacherCourseService.listActiveCourses();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    @GetMapping("/courses/{courseId}/chapters")
    public Map<String, Object> listChapters(@PathVariable("courseId") Long courseId, HttpSession session) {
        Map<String, Object> authError = requireTeacher(session);
        if (authError != null) {
            return authError;
        }
        List<CourseChapter> data = teacherCourseService.listChapters(courseId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    @PostMapping("/courses/{courseId}/chapters")
    public Map<String, Object> addChapter(@PathVariable("courseId") Long courseId,
                                          @RequestBody Map<String, Object> body,
                                          HttpSession session) {
        Map<String, Object> authError = requireTeacher(session);
        if (authError != null) {
            return authError;
        }
        String title = body == null ? null : String.valueOf(body.getOrDefault("title", ""));
        Integer sortNo = parseInteger(body == null ? null : body.get("sortNo"));
        if (title == null || title.trim().isEmpty()) {
            return error("章节标题不能为空");
        }
        if (sortNo == null) {
            sortNo = 99;
        }
        CourseChapter created = teacherCourseService.addChapter(courseId, title, sortNo);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", created);
        return response;
    }

    @PutMapping("/chapters/{chapterId}")
    public Map<String, Object> updateChapter(@PathVariable("chapterId") Long chapterId,
                                             @RequestBody Map<String, Object> body,
                                             HttpSession session) {
        Map<String, Object> authError = requireTeacher(session);
        if (authError != null) {
            return authError;
        }
        String title = body == null ? null : String.valueOf(body.getOrDefault("title", ""));
        Integer sortNo = parseInteger(body == null ? null : body.get("sortNo"));
        teacherCourseService.updateChapter(chapterId, title, sortNo);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    @DeleteMapping("/chapters/{chapterId}")
    public Map<String, Object> deleteChapter(@PathVariable("chapterId") Long chapterId, HttpSession session) {
        Map<String, Object> authError = requireTeacher(session);
        if (authError != null) {
            return authError;
        }
        teacherCourseService.deleteChapter(chapterId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    @GetMapping("/chapters/{chapterId}/resources")
    public Map<String, Object> listChapterResources(@PathVariable("chapterId") Long chapterId, HttpSession session) {
        Map<String, Object> authError = requireTeacher(session);
        if (authError != null) {
            return authError;
        }
        List<Map<String, Object>> data = teacherCourseService.listChapterResources(chapterId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    @PostMapping("/chapters/{chapterId}/resources")
    public Map<String, Object> addChapterResource(@PathVariable("chapterId") Long chapterId,
                                                  @RequestBody Map<String, Object> body,
                                                  HttpSession session) {
        Map<String, Object> authError = requireTeacher(session);
        if (authError != null) {
            return authError;
        }
        String resourceType = body == null ? null : String.valueOf(body.getOrDefault("resourceType", "doc"));
        String title = body == null ? "" : String.valueOf(body.getOrDefault("title", ""));
        String url = body == null ? "" : String.valueOf(body.getOrDefault("url", ""));
        if (url.trim().isEmpty()) {
            return error("资源链接 url 不能为空");
        }
        teacherCourseService.addChapterResource(chapterId, resourceType, title, url);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    @DeleteMapping("/resources/{resourceId}")
    public Map<String, Object> deleteResource(@PathVariable("resourceId") Long resourceId, HttpSession session) {
        Map<String, Object> authError = requireTeacher(session);
        if (authError != null) {
            return authError;
        }
        teacherCourseService.deleteResource(resourceId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    @GetMapping("/courses/{courseId}/chapters/{chapterId}/sections")
    public Map<String, Object> listSections(@PathVariable("courseId") Long courseId,
                                            @PathVariable("chapterId") Long chapterId,
                                            HttpSession session) {
        Map<String, Object> authError = requireTeacher(session);
        if (authError != null) {
            return authError;
        }
        List<CourseSection> data = teacherCourseService.listSections(courseId, chapterId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    @PostMapping("/courses/{courseId}/chapters/{chapterId}/sections")
    public Map<String, Object> addSection(@PathVariable("courseId") Long courseId,
                                          @PathVariable("chapterId") Long chapterId,
                                          @RequestBody Map<String, Object> body,
                                          HttpSession session) {
        Map<String, Object> authError = requireTeacher(session);
        if (authError != null) {
            return authError;
        }
        String title = body == null ? null : String.valueOf(body.getOrDefault("title", ""));
        Integer sortNo = parseInteger(body == null ? null : body.get("sortNo"));
        Integer estimatedMinutes = parseInteger(body == null ? null : body.get("estimatedMinutes"));
        if (title == null || title.trim().isEmpty()) {
            return error("小节标题不能为空");
        }
        CourseSection created = teacherCourseService.addSection(courseId, chapterId, title, sortNo, estimatedMinutes);
        if (created == null) {
            return error("章节不存在或不属于该课程");
        }
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", created);
        return response;
    }

    @PutMapping("/sections/{sectionId}")
    public Map<String, Object> updateSection(@PathVariable("sectionId") Long sectionId,
                                             @RequestBody Map<String, Object> body,
                                             HttpSession session) {
        Map<String, Object> authError = requireTeacher(session);
        if (authError != null) {
            return authError;
        }
        String title = body == null ? null : String.valueOf(body.getOrDefault("title", ""));
        Integer sortNo = parseInteger(body == null ? null : body.get("sortNo"));
        Integer estimatedMinutes = parseInteger(body == null ? null : body.get("estimatedMinutes"));
        teacherCourseService.updateSection(sectionId, title, sortNo, estimatedMinutes);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    @DeleteMapping("/sections/{sectionId}")
    public Map<String, Object> deleteSection(@PathVariable("sectionId") Long sectionId, HttpSession session) {
        Map<String, Object> authError = requireTeacher(session);
        if (authError != null) {
            return authError;
        }
        teacherCourseService.deleteSection(sectionId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    @GetMapping("/sections/{sectionId}/resources")
    public Map<String, Object> listSectionResources(@PathVariable("sectionId") Long sectionId, HttpSession session) {
        Map<String, Object> authError = requireTeacher(session);
        if (authError != null) {
            return authError;
        }
        List<Map<String, Object>> data = teacherCourseService.listSectionResources(sectionId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    @PostMapping("/sections/{sectionId}/resources")
    public Map<String, Object> addSectionResource(@PathVariable("sectionId") Long sectionId,
                                                  @RequestBody Map<String, Object> body,
                                                  HttpSession session) {
        Map<String, Object> authError = requireTeacher(session);
        if (authError != null) {
            return authError;
        }
        String resourceType = body == null ? null : String.valueOf(body.getOrDefault("resourceType", "doc"));
        String title = body == null ? "" : String.valueOf(body.getOrDefault("title", ""));
        String url = body == null ? "" : String.valueOf(body.getOrDefault("url", ""));
        if (url.trim().isEmpty()) {
            return error("资源链接 url 不能为空");
        }
        teacherCourseService.addSectionResource(sectionId, resourceType, title, url);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    private Map<String, Object> requireTeacher(HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"teacher".equals(role)) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("code", 403);
            response.put("message", "仅教师可操作");
            return response;
        }
        return null;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        return response;
    }

    private Integer parseInteger(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
