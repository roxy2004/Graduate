package com.zjut.graduate.Service;

import java.util.Map;

/** 个性化学习路线推荐 */
public interface LearningRouteService {

    /** 获取最新学习路线（含 AI 增强） */
    Map<String, Object> getLatestRoute(Long userId);

    /** 获取最新学习路线；includeAi=false 时仅规则推荐，避免首屏被大模型阻塞 */
    Map<String, Object> getLatestRoute(Long userId, boolean includeAi);
}
