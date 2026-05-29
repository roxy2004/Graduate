package com.zjut.graduate.Controller;

import com.zjut.graduate.Service.PracticeWrongTutorService;
import com.zjut.graduate.Service.StudentPracticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
/** 学生练习与答题 */
@RequestMapping("/xwd/student/practice")
public class StudentPracticeController {

    @Autowired
    private StudentPracticeService studentPracticeService;

    @Autowired
    private PracticeWrongTutorService practiceWrongTutorService;

    /** 知识点练习概览列表 */
    @GetMapping("/knowledge-points")
    public Map<String, Object> listKnowledgePoints(HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) {
            return authError;
        }
        Long userId = (Long) session.getAttribute("userId");
        List<Map<String, Object>> data = studentPracticeService.listKnowledgePointPracticeSummary(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    /** 单个知识点练习摘要 */
    @GetMapping("/knowledge-points/{kpId}/summary")
    public Map<String, Object> kpSummary(@PathVariable("kpId") Long kpId, HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) {
            return authError;
        }
        Long userId = (Long) session.getAttribute("userId");
        Map<String, Object> data = studentPracticeService.getKnowledgePointPracticeSummary(userId, kpId);
        if (data == null) {
            return error("知识点不存在");
        }
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    /** 跨知识点随机题单 */
    @GetMapping("/random-deck")
    public Map<String, Object> randomCrossKpDeck(@RequestParam(value = "limit", defaultValue = "10") int limit,
                                                 HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) {
            return authError;
        }
        Long userId = (Long) session.getAttribute("userId");
        int n = Math.max(1, Math.min(limit, 50));
        List<Map<String, Object>> data = studentPracticeService.getRandomCrossKpPracticeDeck(userId, n);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    /** 每日推荐题单 */
    @GetMapping("/daily-deck")
    public Map<String, Object> dailyDeck(@RequestParam(value = "limit", defaultValue = "10") int limit,
                                         HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) {
            return authError;
        }
        Long userId = (Long) session.getAttribute("userId");
        int n = Math.max(1, Math.min(limit, 50));
        List<Map<String, Object>> data = studentPracticeService.getDailyRecommendedDeck(userId, n);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    /** 指定知识点练习题单 */
    @GetMapping("/knowledge-points/{kpId}/deck")
    public Map<String, Object> practiceDeck(@PathVariable("kpId") Long kpId, HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) {
            return authError;
        }
        Long userId = (Long) session.getAttribute("userId");
        List<Map<String, Object>> data = studentPracticeService.getPracticeDeck(userId, kpId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    /** 获取知识点下一道题 */
    @GetMapping("/knowledge-points/{kpId}/next-question")
    public Map<String, Object> nextQuestion(@PathVariable("kpId") Long kpId, HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) {
            return authError;
        }
        Long userId = (Long) session.getAttribute("userId");
        Map<String, Object> q = studentPracticeService.nextQuestion(userId, kpId);
        if (q == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", null);
            response.put("message", "该知识点下暂无可用题目");
            return response;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", q);
        return response;
    }

    /** 提交作答并记录结果 */
    @PostMapping("/knowledge-points/{kpId}/attempts")
    public Map<String, Object> submitAttempt(@PathVariable("kpId") Long kpId,
                                               @RequestBody Map<String, Object> body,
                                               HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) {
            return authError;
        }
        Long userId = (Long) session.getAttribute("userId");
        Long questionId = parseLong(body == null ? null : body.get("questionId"));
        String userAnswer = body == null ? null : String.valueOf(body.getOrDefault("userAnswer", ""));
        Integer timeSpent = parseInt(body == null ? null : body.get("timeSpent"));
        return studentPracticeService.submitAttempt(userId, kpId, questionId, userAnswer, timeSpent);
    }

    /** 错题 AI 讲解 */
    @PostMapping("/knowledge-points/{kpId}/wrong-tutor")
    public Map<String, Object> wrongTutor(@PathVariable("kpId") Long kpId,
                                          @RequestBody Map<String, Object> body,
                                          HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) {
            return authError;
        }
        Long userId = (Long) session.getAttribute("userId");
        Long questionId = parseLong(body == null ? null : body.get("questionId"));
        String userAnswer = body == null ? null : String.valueOf(body.getOrDefault("userAnswer", ""));
        return practiceWrongTutorService.explainWrongAnswer(userId, kpId, questionId, userAnswer);
    }

    /** 收藏 AI 辅导笔记到学习笔记 */
    @PostMapping("/knowledge-points/{kpId}/favorite-tutor-note")
    public Map<String, Object> favoriteTutorNote(@PathVariable("kpId") Long kpId,
                                                  @RequestBody Map<String, Object> body,
                                                  HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) {
            return authError;
        }
        Long userId = (Long) session.getAttribute("userId");
        Long questionId = parseLong(body == null ? null : body.get("questionId"));
        String content = body == null ? null : String.valueOf(body.getOrDefault("content", ""));
        return practiceWrongTutorService.favoriteTutorNote(userId, kpId, questionId, content);
    }

    /** 清空某知识点下练习记录 */
    @DeleteMapping("/knowledge-points/{kpId}/records")
    public Map<String, Object> clearRecords(@PathVariable("kpId") Long kpId, HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) {
            return authError;
        }
        Long userId = (Long) session.getAttribute("userId");
        studentPracticeService.clearPracticeRecords(userId, kpId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "已清空该知识点下的做题记录");
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

    /** 请求体字段转 Long */
    private Long parseLong(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 请求体字段转 Integer */
    private Integer parseInt(Object raw) {
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
