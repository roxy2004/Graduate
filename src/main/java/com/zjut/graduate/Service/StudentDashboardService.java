package com.zjut.graduate.Service;

import java.util.Map;

/** 学生仪表盘与学习画像 */
public interface StudentDashboardService {

    /** 仪表盘统计数据 */
    Map<String, Object> getDashboardStats(Long userId);

    /** 学习画像（知识点掌握等） */
    Map<String, Object> getLearnerProfile(Long userId);
}
