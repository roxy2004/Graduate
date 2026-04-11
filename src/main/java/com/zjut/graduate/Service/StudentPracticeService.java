package com.zjut.graduate.Service;

import java.util.List;
import java.util.Map;

public interface StudentPracticeService {

    List<Map<String, Object>> listKnowledgePointPracticeSummary(Long userId);

    Map<String, Object> getKnowledgePointPracticeSummary(Long userId, Long kpId);

    Map<String, Object> nextQuestion(Long userId, Long kpId);

    /**
     * 整卷题目（卡片滑动），含每题最近一次作答摘要。
     */
    List<Map<String, Object>> getPracticeDeck(Long userId, Long kpId);

    Map<String, Object> submitAttempt(Long userId, Long kpId, Long questionId, String userAnswer, Integer timeSpent);

    void clearPracticeRecords(Long userId, Long kpId);
}
