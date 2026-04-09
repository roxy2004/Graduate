package com.zjut.graduate.Po;

import java.util.Date;

public class QuestionBank {
    private Long id;
    private String content;           // 题干
    private String imageUrl;          // 配图URL，可为null
    private String options;           // JSON字符串，存储选项
    private String correctAnswer;     // 'A','B','C','D'
    private Double difficulty;        // 0~1
    private String knowledgePointIds; // 逗号分隔，如 "1,3,5"
    private Date createdAt;
}
