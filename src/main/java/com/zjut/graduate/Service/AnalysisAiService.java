package com.zjut.graduate.Service;

import java.util.Map;

public interface AnalysisAiService {
    String analyzeMistake(String questionContent, String userAnswer, String correctAnswer);

    Map<String, Object> buildLearnerProfileSummary(Long userId);
}
