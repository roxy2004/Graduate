package com.zjut.graduate.Po;

public class KnowledgePoint {
    private Long id;
    private String name;
    /** 专项学习小节 id：收藏练习 AI 笔记时落点；可为空，后端会尝试按小节标题=知识点名匹配 */
    private Long anchorSectionId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getAnchorSectionId() {
        return anchorSectionId;
    }

    public void setAnchorSectionId(Long anchorSectionId) {
        this.anchorSectionId = anchorSectionId;
    }
}