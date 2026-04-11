package com.zjut.graduate.Po;

import java.util.Date;

/**
 * 章节维度查看笔记：附带所属小节标题，便于阅读。
 */
public class UserLearningChapterNoteRow {
    private Long id;
    private Long sectionId;
    private String sectionTitle;
    private String noteContent;
    private Integer noteTimeSec;
    private Date createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }

    public String getNoteContent() {
        return noteContent;
    }

    public void setNoteContent(String noteContent) {
        this.noteContent = noteContent;
    }

    public Integer getNoteTimeSec() {
        return noteTimeSec;
    }

    public void setNoteTimeSec(Integer noteTimeSec) {
        this.noteTimeSec = noteTimeSec;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
