package com.zjut.graduate.Controller;

import com.zjut.graduate.Po.QuestionBank;
import com.zjut.graduate.Service.QuestionBankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/xwd/questions")
public class QuestionManageController {

    @Autowired
    private QuestionBankService questionBankService;

    @PostMapping("/import")
    public Map<String, Object> importQuestions(@RequestParam("file") MultipartFile file, HttpSession session) {
        Map<String, Object> authError = requireTeacher(session);
        if (authError != null) {
            return authError;
        }
        if (file == null || file.isEmpty()) {
            return error("请上传CSV文件");
        }
        Long userId = (Long) session.getAttribute("userId");
        int count = questionBankService.importQuestionsFromCsv(file, userId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "导入成功");
        response.put("count", count);
        return response;
    }

    @GetMapping
    public Map<String, Object> listQuestions(HttpSession session) {
        Map<String, Object> authError = requireTeacher(session);
        if (authError != null) {
            return authError;
        }
        List<Map<String, Object>> data = questionBankService.listAllQuestions()
                .stream()
                .map(this::sanitizeQuestion)
                .collect(Collectors.toList());
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteQuestion(@PathVariable("id") Long id, HttpSession session) {
        Map<String, Object> authError = requireTeacher(session);
        if (authError != null) {
            return authError;
        }
        boolean deleted = questionBankService.deleteQuestion(id);
        if (!deleted) {
            return error("删除失败，题目不存在");
        }
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "题目删除成功");
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

    private Map<String, Object> sanitizeQuestion(QuestionBank question) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", question.getId());
        map.put("content", question.getContent());
        map.put("imageUrl", question.getImageUrl());
        map.put("options", question.getOptions());
        map.put("correctAnswer", question.getCorrectAnswer());
        map.put("difficulty", question.getDifficulty());
        map.put("questionType", question.getQuestionType());
        map.put("sourceTag", question.getSourceTag());
        map.put("status", question.getStatus());
        map.put("knowledgePointIds", question.getKnowledgePointIds());
        map.put("createdBy", question.getCreatedBy());
        map.put("createdAt", question.getCreatedAt());
        return map;
    }
}
