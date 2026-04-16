package com.zjut.graduate.Service.impl;

import com.zjut.graduate.Service.LearningRouteService;
import com.zjut.graduate.Service.StudentDashboardService;
import com.zjut.graduate.Service.StudentHomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StudentHomeServiceImpl implements StudentHomeService {

    @Autowired
    private StudentDashboardService studentDashboardService;

    @Autowired
    private LearningRouteService learningRouteService;

    @Override
    public Map<String, Object> getOverview(Long userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("stats", studentDashboardService.getDashboardStats(userId));
        // 首页走轻量推荐，避免登录后首屏被 AI 调用阻塞。
        response.put("recommendation", learningRouteService.getLatestRoute(userId, false));
        return response;
    }
}

