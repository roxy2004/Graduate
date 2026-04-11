package com.zjut.graduate.Po;

public class CourseSection {
    private Long id;
    /** 教师维护小节时写入；学生端列表查询不映射此字段时可忽略 */
    private Long courseId;
    private Long chapterId;
    private Integer chapterSortNo;
    private String title;
    private String sectionType;
    private Integer durationSeconds;
    private Integer sortNo;
    private String chapterTitle;
    private Integer learnedSeconds;
    private Integer noteCount;
    /** 预估学习时长（分钟），教师端小节表单使用 */
    private Integer estimatedMinutes;

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(Integer estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public Integer getChapterSortNo() {
        return chapterSortNo;
    }

    public void setChapterSortNo(Integer chapterSortNo) {
        this.chapterSortNo = chapterSortNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSectionType() {
        return sectionType;
    }

    public void setSectionType(String sectionType) {
        this.sectionType = sectionType;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    public Integer getLearnedSeconds() {
        return learnedSeconds;
    }

    public void setLearnedSeconds(Integer learnedSeconds) {
        this.learnedSeconds = learnedSeconds;
    }

    public Integer getNoteCount() {
        return noteCount;
    }

    public void setNoteCount(Integer noteCount) {
        this.noteCount = noteCount;
    }
}

