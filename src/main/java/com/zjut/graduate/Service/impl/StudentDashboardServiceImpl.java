package com.zjut.graduate.Service.impl;

import com.zjut.graduate.Dao.KnowledgePointDao;
import com.zjut.graduate.Dao.LearnerKnowledgeStateDao;
import com.zjut.graduate.Dao.LearningRecordDao;
import com.zjut.graduate.Po.KnowledgePoint;
import com.zjut.graduate.Po.LearnerKnowledgeState;
import com.zjut.graduate.Service.StudentDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudentDashboardServiceImpl implements StudentDashboardService {

    @Autowired
    private LearningRecordDao learningRecordDao;

    @Autowired
    private LearnerKnowledgeStateDao learnerKnowledgeStateDao;

    @Autowired
    private KnowledgePointDao knowledgePointDao;

    @Override
    public Map<String, Object> getDashboardStats(Long userId) {
        int solvedCount = learningRecordDao.countByUserId(userId);
        int correctCount = learningRecordDao.countCorrectByUserId(userId);
        int learningDays = learningRecordDao.countLearningDaysByUserId(userId);
        double accuracy = solvedCount == 0 ? 0D : (correctCount * 100D / solvedCount);

        Map<String, Object> data = new HashMap<>();
        data.put("solvedCount", solvedCount);
        data.put("correctCount", correctCount);
        data.put("accuracy", Math.round(accuracy * 10D) / 10D);
        data.put("learningDays", learningDays);
        return data;
    }

    @Override
    public Map<String, Object> getLearnerProfile(Long userId) {
        List<LearnerKnowledgeState> states = learnerKnowledgeStateDao.selectByUserId(userId);
        List<Map<String, Object>> kpStats = learningRecordDao.selectKnowledgePracticeStatsByUserId(userId);
        List<KnowledgePoint> allKps = knowledgePointDao.selectAll();

        Map<Long, String> kpNameMap = new HashMap<>();
        for (KnowledgePoint kp : allKps) {
            if (kp != null && kp.getId() != null) {
                kpNameMap.put(kp.getId(), kp.getName());
            }
        }

        Map<Long, Map<String, Object>> statsByKp = new HashMap<>();
        for (Map<String, Object> row : kpStats) {
            Long kpId = toLong(row.get("kpId"));
            if (kpId == null) {
                continue;
            }
            statsByKp.put(kpId, row);
        }

        List<Map<String, Object>> points = new ArrayList<>();
        for (LearnerKnowledgeState st : states) {
            if (st == null || st.getKpId() == null) {
                continue;
            }
            Long kpId = st.getKpId();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("kpId", kpId);
            row.put("kpName", kpNameMap.getOrDefault(kpId, "知识点#" + kpId));
            double mastery = clamp01(st.getMasteryLevel());
            row.put("mastery", mastery);
            row.put("masteryPercent", Math.round(mastery * 1000D) / 10D);
            double confidence = clamp01(st.getConfidence());
            row.put("confidence", confidence);
            row.put("confidencePercent", Math.round(confidence * 1000D) / 10D);
            row.put("lastPracticedAt", st.getLastPracticedAt());

            Map<String, Object> agg = statsByKp.get(kpId);
            int practiced = toInt(agg == null ? null : agg.get("practicedCount"));
            int correct = toInt(agg == null ? null : agg.get("correctCount"));
            row.put("practicedCount", practiced);
            row.put("correctCount", correct);
            row.put("accuracy", practiced <= 0 ? 0D : Math.round(correct * 1000D / practiced) / 10D);
            points.add(row);
        }

        points.sort(Comparator.comparingDouble(a -> toDouble(a.get("mastery"))));

        int weakCount = Math.min(3, points.size());
        List<Map<String, Object>> weakTop = new ArrayList<>(points.subList(0, weakCount));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("knowledgePoints", points);
        data.put("weakTop", weakTop);
        data.put("knowledgePointCount", points.size());
        return data;
    }

    private static double clamp01(Double v) {
        if (v == null) {
            return 0D;
        }
        if (v < 0D) {
            return 0D;
        }
        if (v > 1D) {
            return 1D;
        }
        return v;
    }

    private static Double toDouble(Object raw) {
        if (raw instanceof Number) {
            return ((Number) raw).doubleValue();
        }
        if (raw == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (Exception e) {
            return null;
        }
    }

    private static Long toLong(Object raw) {
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        if (raw == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(raw));
        } catch (Exception e) {
            return null;
        }
    }

    private static int toInt(Object raw) {
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        if (raw == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (Exception e) {
            return 0;
        }
    }
}

