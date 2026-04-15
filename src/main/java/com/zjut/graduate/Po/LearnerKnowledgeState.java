package com.zjut.graduate.Po;

import java.util.Date;

public class LearnerKnowledgeState {
    private Long id;
    private Long userId;
    private Long kpId;
    private Double masteryLevel;   // 0~1
    private Double confidence;     // 0~1
    private Date lastPracticedAt;
    private Date updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getKpId() {
        return kpId;
    }

    public void setKpId(Long kpId) {
        this.kpId = kpId;
    }

    public Double getMasteryLevel() {
        return masteryLevel;
    }

    public void setMasteryLevel(Double masteryLevel) {
        this.masteryLevel = masteryLevel;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Date getLastPracticedAt() {
        return lastPracticedAt;
    }

    public void setLastPracticedAt(Date lastPracticedAt) {
        this.lastPracticedAt = lastPracticedAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
