package com.zjut.graduate.Controller;

import com.zjut.graduate.Service.DeepSeekProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/xwd/student/deepseek")
public class StudentDeepSeekController {

    @Autowired
    private DeepSeekProxyService deepSeekProxyService;

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> body, HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) {
            return authError;
        }
        Object raw = body == null ? null : body.get("messages");
        if (!(raw instanceof List)) {
            return error("请求体需包含 messages 数组");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) raw;
        try {
            String reply = deepSeekProxyService.chat(messages);
            Map<String, Object> ok = new HashMap<>();
            ok.put("status", "success");
            ok.put("message", reply);
            return ok;
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        } catch (IllegalStateException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            return error("对话失败: " + e.getMessage());
        }
    }

    private Map<String, Object> requireStudent(HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"student".equals(role)) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("code", 403);
            response.put("message", "仅学生可使用学习助手");
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
