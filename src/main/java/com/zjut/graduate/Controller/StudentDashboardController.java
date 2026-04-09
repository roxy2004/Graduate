package com.zjut.graduate.Controller;

import com.zjut.graduate.Dao.LearningRecordDao;
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
@RequestMapping("/xwd/student")
public class StudentDashboardController {

    @Autowired
    private LearningRecordDao learningRecordDao;

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardStats(HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) {
            return authError;
        }
        Long userId = (Long) session.getAttribute("userId");
        int solvedCount = learningRecordDao.countByUserId(userId);
        int correctCount = learningRecordDao.countCorrectByUserId(userId);
        int learningDays = learningRecordDao.countLearningDaysByUserId(userId);
        double accuracy = solvedCount == 0 ? 0D : (correctCount * 100D / solvedCount);

        Map<String, Object> data = new HashMap<>();
        data.put("solvedCount", solvedCount);
        data.put("correctCount", correctCount);
        data.put("accuracy", Math.round(accuracy * 10D) / 10D);
        data.put("learningDays", learningDays);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
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
