package com.zjut.graduate.Service.impl;

import com.zjut.graduate.Dao.KnowledgePointDao;
import com.zjut.graduate.Dao.LearningRecordDao;
import com.zjut.graduate.Dao.QuestionBankDao;
import com.zjut.graduate.Dao.QuestionKnowledgePointRelDao;
import com.zjut.graduate.Po.KnowledgePoint;
import com.zjut.graduate.Po.QuestionBank;
import com.zjut.graduate.Service.StudentPracticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudentPracticeServiceImpl implements StudentPracticeService {

    @Autowired
    private KnowledgePointDao knowledgePointDao;

    @Autowired
    private QuestionBankDao questionBankDao;

    @Autowired
    private QuestionKnowledgePointRelDao questionKnowledgePointRelDao;

    @Autowired
    private LearningRecordDao learningRecordDao;

    @Override
    public List<Map<String, Object>> listKnowledgePointPracticeSummary(Long userId) {
        return knowledgePointDao.selectPracticeSummaryByUser(userId);
    }

    @Override
    public Map<String, Object> getKnowledgePointPracticeSummary(Long userId, Long kpId) {
        KnowledgePoint kp = knowledgePointDao.selectById(kpId);
        if (kp == null) {
            return null;
        }
        List<Map<String, Object>> all = knowledgePointDao.selectPracticeSummaryByUser(userId);
        for (Map<String, Object> row : all) {
            Object id = row.get("id");
            if (id instanceof Number && ((Number) id).longValue() == kpId) {
                return row;
            }
        }
        Map<String, Object> empty = new LinkedHashMap<>();
        empty.put("id", kpId);
        empty.put("name", kp.getName());
        empty.put("totalQuestions", 0);
        empty.put("practicedQuestions", 0);
        return empty;
    }

    @Override
    public List<Map<String, Object>> getPracticeDeck(Long userId, Long kpId) {
        if (knowledgePointDao.selectById(kpId) == null) {
            return java.util.Collections.emptyList();
        }
        List<Map<String, Object>> raw = questionBankDao.selectPracticeDeckRows(kpId, userId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : raw) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", col(row, "id"));
            item.put("content", col(row, "content"));
            item.put("questionType", col(row, "questionType"));
            item.put("options", col(row, "options"));
            item.put("difficulty", col(row, "difficulty"));
            item.put("sourceTag", col(row, "sourceTag"));
            Object lua = col(row, "lastUserAnswer");
            Object lic = col(row, "lastIsCorrect");
            Object lat = col(row, "lastAnsweredAt");
            Object lts = col(row, "lastTimeSpent");
            Object ca = col(row, "correctAnswer");
            if (lua != null && !String.valueOf(lua).trim().isEmpty()) {
                Map<String, Object> la = new LinkedHashMap<>();
                la.put("userAnswer", String.valueOf(lua).trim().toUpperCase());
                boolean ok = false;
                if (lic instanceof Number) {
                    ok = ((Number) lic).intValue() == 1;
                } else if (lic instanceof Boolean) {
                    ok = (Boolean) lic;
                }
                la.put("isCorrect", ok);
                la.put("answeredAt", lat);
                Integer priorSec = null;
                if (lts instanceof Number) {
                    priorSec = Math.max(0, ((Number) lts).intValue());
                }
                la.put("timeSpent", priorSec);
                item.put("lastAttempt", la);
                item.put("correctAnswer", ca == null ? null : String.valueOf(ca).trim().toUpperCase());
            } else {
                item.put("lastAttempt", null);
                item.put("correctAnswer", null);
            }
            out.add(item);
        }
        return out;
    }

    private static Object col(Map<String, Object> row, String key) {
        if (row.containsKey(key)) {
            return row.get(key);
        }
        for (Map.Entry<String, Object> e : row.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
                return e.getValue();
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> nextQuestion(Long userId, Long kpId) {
        if (knowledgePointDao.selectById(kpId) == null) {
            return null;
        }
        QuestionBank qb = questionBankDao.selectNextPracticeQuestion(kpId, userId);
        if (qb == null) {
            return null;
        }
        return toPublicQuestion(qb);
    }

    @Override
    @Transactional
    public Map<String, Object> submitAttempt(Long userId, Long kpId, Long questionId, String userAnswer, Integer timeSpent) {
        Map<String, Object> out = new HashMap<>();
        if (questionId == null || questionKnowledgePointRelDao.countByQuestionAndKp(questionId, kpId) <= 0) {
            out.put("status", "error");
            out.put("message", "题目不属于该知识点");
            return out;
        }
        QuestionBank qb = questionBankDao.selectById(questionId);
        if (qb == null || qb.getStatus() == null || qb.getStatus() != 1) {
            out.put("status", "error");
            out.put("message", "题目不可用");
            return out;
        }
        String ua = userAnswer == null ? "" : userAnswer.trim().toUpperCase();
        if (ua.isEmpty()) {
            out.put("status", "error");
            out.put("message", "请选择答案");
            return out;
        }
        String correct = qb.getCorrectAnswer() == null ? "" : qb.getCorrectAnswer().trim();
        int isCorrect = correct.equalsIgnoreCase(ua) ? 1 : 0;
        int ts = timeSpent == null ? 0 : Math.max(0, Math.min(timeSpent, 3600));
        int attemptNo = learningRecordDao.selectMaxAttemptNo(userId, questionId) + 1;
        learningRecordDao.insertPracticeAttempt(userId, questionId, ua, isCorrect, ts, attemptNo);
        out.put("status", "success");
        out.put("isCorrect", isCorrect == 1);
        out.put("correctAnswer", correct.toUpperCase());
        return out;
    }

    @Override
    @Transactional
    public void clearPracticeRecords(Long userId, Long kpId) {
        if (knowledgePointDao.selectById(kpId) == null) {
            return;
        }
        learningRecordDao.deleteMistakeAnalysisByUserAndKp(userId, kpId);
        learningRecordDao.deleteLearningRecordsByUserAndKp(userId, kpId);
    }

    private Map<String, Object> toPublicQuestion(QuestionBank qb) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", qb.getId());
        m.put("content", qb.getContent());
        m.put("questionType", qb.getQuestionType());
        m.put("options", qb.getOptions());
        m.put("difficulty", qb.getDifficulty());
        m.put("sourceTag", qb.getSourceTag());
        m.put("knowledgePointIds", qb.getKnowledgePointIds());
        return m;
    }
}
