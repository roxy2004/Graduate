package com.zjut.graduate.Service;

import java.util.Map;

public interface LearningRouteService {
    Map<String, Object> getLatestRoute(Long userId);

    /**
     * includeAi=false 时仅返回规则推荐，避免首屏加载被大模型调用阻塞。
     */
    Map<String, Object> getLatestRoute(Long userId, boolean includeAi);
}

