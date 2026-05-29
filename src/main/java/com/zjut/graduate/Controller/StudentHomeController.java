package com.zjut.graduate.Controller;

import com.zjut.graduate.Service.StudentHomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
/** 学生首页聚合数据 */
@RequestMapping("/xwd/student/home")
public class StudentHomeController {

    @Autowired
    private StudentHomeService studentHomeService;

    /** 学生首页概览（统计 + 轻量推荐） */
    @GetMapping("/overview")
    public Map<String, Object> overview(HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        Long userId = (Long) session.getAttribute("userId");
        Map<String, Object> data = studentHomeService.getOverview(userId);
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

