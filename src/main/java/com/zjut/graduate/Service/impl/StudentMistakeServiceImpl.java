package com.zjut.graduate.Service.impl;

import com.zjut.graduate.Dao.LearningRecordDao;
import com.zjut.graduate.Service.StudentMistakeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudentMistakeServiceImpl implements StudentMistakeService {

    @Autowired
    private LearningRecordDao learningRecordDao;

    @Override
    public List<Map<String, Object>> listMistakes(Long userId) {
        return learningRecordDao.selectMistakesByUserId(userId);
    }

    @Override
    public Map<String, Object> redo(Long userId, Long recordId, String answer) {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> detail = learningRecordDao.selectMistakeDetailById(recordId);
        if (detail == null) {
            result.put("status", "error");
            result.put("message", "记录不存在");
            return result;
        }
        Object ownerId = detail.get("user_id");
        if (!(ownerId instanceof Number) || ((Number) ownerId).longValue() != userId) {
            result.put("status", "error");
            result.put("message", "无权操作该记录");
            return result;
        }

        String correctAnswer = detail.get("correct_answer") == null ? "" : detail.get("correct_answer").toString();
        boolean isCorrect = correctAnswer.equalsIgnoreCase(answer.trim());
        learningRecordDao.updateRedoResult(recordId, userId, answer.trim().toUpperCase(), isCorrect ? 1 : 0);

        result.put("status", "success");
        result.put("isCorrect", isCorrect);
        result.put("message", isCorrect ? "重做正确，已从错题本移除" : "重做仍错误，已更新你的作答");
        return result;
    }
}

