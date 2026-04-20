package com.zjut.graduate.Service.impl;

import com.zjut.graduate.Dao.LearningRouteDao;
import com.zjut.graduate.Po.LearningRoute;
import com.zjut.graduate.Po.LearningRouteItem;
import com.zjut.graduate.Service.LearningRouteMaintenanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class LearningRouteMaintenanceServiceImpl implements LearningRouteMaintenanceService {

    /**
     * 规则引擎同步的个性化路线；需数据库 route_type 为 VARCHAR（或含该值的 ENUM）。种子里的 demo 路线仍可为 weakness 等。
     */
    private static final String ROUTE_TYPE_SYNC = "personalized";
    private static final String ROUTE_STATUS = "active";

    /**
     * 个性化路线步骤：item_id 为知识点 id。需 item_type 为 VARCHAR（或含该值的 ENUM）。
     * 若仍为仅含 section 的旧 ENUM，请执行 sql/migrate_learning_route_item_item_type_varchar.sql 或暂改为 "section"。
     */
    private static final String ITEM_TYPE_DB = "knowledge_point";

    @Autowired
    private LearningRouteDao learningRouteDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncFromPersonalized(Long userId, List<Map<String, Object>> personalized) {
        if (userId == null || personalized == null || personalized.isEmpty()) {
            return;
        }
        String dayKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String title = "个性化学习路线 · " + dayKey;
        String summary = String.format("系统根据掌握度、正确率与复习间隔生成 %d 个优先知识点，建议按顺序完成练习。", personalized.size());

        Date now = new Date();
        LearningRoute latest = learningRouteDao.selectLatestByUserId(userId);
        if (latest == null) {
            latest = new LearningRoute();
            latest.setUserId(userId);
            latest.setTitle(title);
            latest.setRouteType(ROUTE_TYPE_SYNC);
            latest.setStatus(ROUTE_STATUS);
            latest.setGeneratedBy("rule_engine");
            latest.setSummary(summary);
            latest.setCreatedAt(now);
            learningRouteDao.insertRoute(latest);
        } else {
            latest.setTitle(title);
            latest.setSummary(summary);
            latest.setRouteType(ROUTE_TYPE_SYNC);
            latest.setStatus(ROUTE_STATUS);
            latest.setGeneratedBy("rule_engine");
            learningRouteDao.updateRouteMeta(latest);
            learningRouteDao.deleteItemsByRouteId(latest.getId());
        }

        int sort = 1;
        for (Map<String, Object> p : personalized) {
            Long kpId = toLong(p.get("kpId"));
            if (kpId == null) {
                continue;
            }
            LearningRouteItem item = new LearningRouteItem();
            item.setRouteId(latest.getId());
            item.setItemType(ITEM_TYPE_DB);
            item.setItemId(kpId);
            item.setReason(String.valueOf(p.getOrDefault("reason", "建议巩固该知识点")));
            item.setPriority((int) Math.round(clamp01(toDouble(p.get("priorityScore"))) * 100));
            int remaining = toInt(p.get("remainingQuestions"));
            item.setEstimatedMinutes(Math.max(10, Math.min(45, 10 + remaining / 2)));
            item.setSortNo(sort++);
            double mastery = toDouble(p.get("mastery"));
            item.setCompleted(mastery >= 0.75 ? 1 : 0);
            learningRouteDao.insertItem(item);
        }
    }

    private static Long toLong(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int toInt(Object raw) {
        if (raw == null) {
            return 0;
        }
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double toDouble(Object raw) {
        if (raw == null) {
            return 0D;
        }
        if (raw instanceof Number) {
            return ((Number) raw).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            return 0D;
        }
    }

    private static double clamp01(double v) {
        return Math.max(0D, Math.min(1D, v));
    }
}
