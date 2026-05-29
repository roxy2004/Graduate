package com.zjut.graduate.Service;

import java.util.List;
import java.util.Map;

/** 学生练习与答题 */
public interface StudentPracticeService {

    /** 各知识点练习概览 */
    List<Map<String, Object>> listKnowledgePointPracticeSummary(Long userId);

    /** 单个知识点练习摘要 */
    Map<String, Object> getKnowledgePointPracticeSummary(Long userId, Long kpId);

    /** 获取下一道练习题 */
    Map<String, Object> nextQuestion(Long userId, Long kpId);

    /** 知识点整卷题单（含最近作答摘要） */
    List<Map<String, Object>> getPracticeDeck(Long userId, Long kpId);

    /** 跨知识点随机小卷 */
    List<Map<String, Object>> getRandomCrossKpPracticeDeck(Long userId, int limit);

    /** 每日推荐固定题单 */
    List<Map<String, Object>> getDailyRecommendedDeck(Long userId, int limit);

    /** 提交作答并记录结果 */
    Map<String, Object> submitAttempt(Long userId, Long kpId, Long questionId, String userAnswer, Integer timeSpent);

    /** 清空某知识点下的练习记录 */
    void clearPracticeRecords(Long userId, Long kpId);
}
