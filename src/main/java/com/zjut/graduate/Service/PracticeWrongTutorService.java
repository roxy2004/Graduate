package com.zjut.graduate.Service;

import java.util.Map;

/** 练习错题 AI 辅导 */
public interface PracticeWrongTutorService {

    /** 错题 AI 讲解 */
    Map<String, Object> explainWrongAnswer(Long userId, Long kpId, Long questionId, String userAnswer);

    /** 收藏 AI 辅导内容为学习笔记 */
    Map<String, Object> favoriteTutorNote(Long userId, Long kpId, Long questionId, String noteBody);
}
