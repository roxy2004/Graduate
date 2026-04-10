package com.zjut.graduate.Service;

import java.util.Map;

public interface StudentDashboardService {
    Map<String, Object> getDashboardStats(Long userId);
}

