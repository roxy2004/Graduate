package com.zjut.graduate.Service;

import java.util.Map;

public interface PracticeWrongTutorService {

    Map<String, Object> explainWrongAnswer(Long userId, Long kpId, Long questionId, String userAnswer);

    Map<String, Object> favoriteTutorNote(Long userId, Long kpId, Long questionId, String noteBody);
}
