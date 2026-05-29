package com.zjut.graduate.Controller;

import com.zjut.graduate.Service.StudentLearningRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
/** 学生做题记录管理 */
@RequestMapping("/xwd/student/records")
public class StudentLearningRecordController {

    @Autowired
    private StudentLearningRecordService studentLearningRecordService;

    /** 查询全部做题记录 */
    @GetMapping
    public Map<String, Object> listRecords(HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        Long userId = (Long) session.getAttribute("userId");
        List<Map<String, Object>> records = studentLearningRecordService.listRecords(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", records);
        return response;
    }

    /** 删除单条做题记录 */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteRecord(@PathVariable("id") Long id, HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        Long userId = (Long) session.getAttribute("userId");
        boolean deleted = studentLearningRecordService.deleteRecord(userId, id);
        if (!deleted) return error("删除失败，记录不存在");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "删除成功");
        return response;
    }

    /** 清空全部做题记录 */
    @DeleteMapping
    public Map<String, Object> clearAll(HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        Long userId = (Long) session.getAttribute("userId");
        int affected = studentLearningRecordService.clearAll(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "已清空");
        response.put("count", affected);
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

    /** 构造错误响应 */
    private Map<String, Object> error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        return response;
    }
}

