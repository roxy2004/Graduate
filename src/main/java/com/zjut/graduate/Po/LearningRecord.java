package com.zjut.graduate.Po;

import java.util.Date;

public class LearningRecord {
    private Long id;
    private Long userId;
    private Long questionId;
    private String userAnswer;    // 'A','B','C','D'
    private Integer isCorrect;    // 0或1
    private Integer timeSpent;    // 秒
    private Date createdAt;
}
