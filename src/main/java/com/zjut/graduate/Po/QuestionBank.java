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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public Double getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Double difficulty) {
        this.difficulty = difficulty;
    }

    public String getKnowledgePointIds() {
        return knowledgePointIds;
    }

    public void setKnowledgePointIds(String knowledgePointIds) {
        this.knowledgePointIds = knowledgePointIds;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
