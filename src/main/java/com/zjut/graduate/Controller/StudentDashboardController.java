package com.zjut.graduate.Controller;

import com.zjut.graduate.Service.StudentDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
/** 学生仪表盘与学习画像 */
@RequestMapping("/xwd/student")
public class StudentDashboardController {

    @Autowired
    private StudentDashboardService studentDashboardService;

    /** 学生仪表盘统计数据 */
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardStats(HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) {
            return authError;
        }
        Long userId = (Long) session.getAttribute("userId");
        Map<String, Object> data = studentDashboardService.getDashboardStats(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    /** 学生学习画像数据 */
    @GetMapping("/profile")
    public Map<String, Object> getLearnerProfile(HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) {
            return authError;
        }
        Long userId = (Long) session.getAttribute("userId");
        Map<String, Object> data = studentDashboardService.getLearnerProfile(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    /** 校验学生权限 */
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
