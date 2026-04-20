package com.zjut.graduate.Service;

import java.util.List;
import java.util.Map;

/**
 * 将个性化推荐结果同步为可持久化的学习路线步骤（写入 learning_route / learning_route_item）。
 */
public interface LearningRouteMaintenanceService {

    /**
     * 根据当前个性化知识点列表更新该用户最新一条学习路线；若不存在则新建。
     */
    void syncFromPersonalized(Long userId, List<Map<String, Object>> personalized);
}
