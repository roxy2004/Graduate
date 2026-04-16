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

    /**
     * 跨知识点随机小卷（默认 10 题），每题 map 含 knowledgePointId、knowledgePointName，供前端按题提交。
     */
    List<Map<String, Object>> getRandomCrossKpPracticeDeck(Long userId, int limit);

    /**
     * 每日推荐固定题单（默认 10 题），来自推荐快照，包含题目选项与最近作答信息。
     */
    List<Map<String, Object>> getDailyRecommendedDeck(Long userId, int limit);

    Map<String, Object> submitAttempt(Long userId, Long kpId, Long questionId, String userAnswer, Integer timeSpent);

    void clearPracticeRecords(Long userId, Long kpId);
}
