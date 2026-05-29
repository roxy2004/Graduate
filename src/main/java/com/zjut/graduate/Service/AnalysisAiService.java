package com.zjut.graduate.Service;

import java.util.Map;

/** AI 错题分析与画像摘要（占位/扩展） */
public interface AnalysisAiService {

    /** 分析单道错题 */
    String analyzeMistake(String questionContent, String userAnswer, String correctAnswer);

    /** 生成学习者画像摘要 */
    Map<String, Object> buildLearnerProfileSummary(Long userId);
}
