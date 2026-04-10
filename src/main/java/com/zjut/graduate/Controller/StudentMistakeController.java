package com.zjut.graduate.Controller;

import com.zjut.graduate.Service.StudentMistakeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/xwd/student/mistakes")
public class StudentMistakeController {

    @Autowired
    private StudentMistakeService studentMistakeService;

    @GetMapping
    public Map<String, Object> listMistakes(HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        Long userId = (Long) session.getAttribute("userId");
        List<Map<String, Object>> data = studentMistakeService.listMistakes(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    @PostMapping("/{recordId}/redo")
    public Map<String, Object> redo(@PathVariable("recordId") Long recordId,
                                    @RequestBody Map<String, String> payload,
                                    HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        Long userId = (Long) session.getAttribute("userId");
        String answer = payload.get("answer");
        if (answer == null || answer.trim().isEmpty()) {
            return error("请选择重做答案");
        }
        return studentMistakeService.redo(userId, recordId, answer);
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

    private Map<String, Object> error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        return response;
    }
}

