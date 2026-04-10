package com.zjut.graduate.Controller;

import com.zjut.graduate.Dao.LearningRecordDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/xwd/student/records")
public class StudentLearningRecordController {

    @Autowired
    private LearningRecordDao learningRecordDao;

    @GetMapping
    public Map<String, Object> listRecords(HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        Long userId = (Long) session.getAttribute("userId");
        List<Map<String, Object>> records = learningRecordDao.selectWithQuestionContentByUserId(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", records);
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteRecord(@PathVariable("id") Long id, HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        Long userId = (Long) session.getAttribute("userId");
        int affected = learningRecordDao.deleteByIdAndUserId(id, userId);
        if (affected <= 0) return error("删除失败，记录不存在");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "删除成功");
        return response;
    }

    @DeleteMapping
    public Map<String, Object> clearAll(HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        Long userId = (Long) session.getAttribute("userId");
        int affected = learningRecordDao.deleteAllByUserId(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "已清空");
        response.put("count", affected);
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

    private Map<String, Object> error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        return response;
    }
}

