package com.zjut.graduate.Po;

import java.util.Date;

public class UserLearningNoteRow {
    private Long id;
    private String noteContent;
    private Integer noteTimeSec;
    private Date createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
