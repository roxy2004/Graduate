package com.zjut.graduate.Controller;

import com.zjut.graduate.Service.LearningRouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
/** 学生学习路线推荐 */
@RequestMapping("/xwd/student/recommendations")
public class StudentRecommendationController {

    @Autowired
    private LearningRouteService learningRouteService;

    /** 获取最新学习路线推荐（可选 AI） */
    @GetMapping("/latest")
    public Map<String, Object> latest(@RequestParam(value = "includeAi", defaultValue = "true") boolean includeAi,
                                      HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        Long userId = (Long) session.getAttribute("userId");
        Map<String, Object> data = learningRouteService.getLatestRoute(userId, includeAi);
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

