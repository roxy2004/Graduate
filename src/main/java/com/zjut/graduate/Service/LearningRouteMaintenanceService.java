package com.zjut.graduate.Service;

import java.util.List;
import java.util.Map;

/** 将推荐结果同步为持久化学习路线（learning_route / learning_route_item） */
public interface LearningRouteMaintenanceService {

    /** 按个性化知识点列表更新或新建用户最新学习路线 */
    void syncFromPersonalized(Long userId, List<Map<String, Object>> personalized);
}
