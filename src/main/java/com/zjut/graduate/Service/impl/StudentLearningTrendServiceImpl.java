package com.zjut.graduate.Service.impl;

import com.zjut.graduate.Dao.KnowledgePointDao;
import com.zjut.graduate.Dao.LearningRecordDao;
import com.zjut.graduate.Po.KnowledgePoint;
import com.zjut.graduate.Service.StudentLearningTrendService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class StudentLearningTrendServiceImpl implements StudentLearningTrendService {

    private static final int TOP_KP_LINES = 6;
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final LearningRecordDao learningRecordDao;
    private final KnowledgePointDao knowledgePointDao;

    public StudentLearningTrendServiceImpl(LearningRecordDao learningRecordDao,
                                           KnowledgePointDao knowledgePointDao) {
        this.learningRecordDao = learningRecordDao;
        this.knowledgePointDao = knowledgePointDao;
    }

    @Override
    public Map<String, Object> buildTrendPayload(Long userId) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<Map<String, Object>> overall = buildOverallAccuracySeries(userId);
        out.put("overallAccuracy", overall);

        List<Long> topKpIds = resolveTopKpIds(userId);
        List<Map<String, Object>> kpTrends = buildKnowledgePointTrends(userId, topKpIds);
        out.put("knowledgePointTrends", kpTrends);

        out.put("hint", "知识点折线为按作答记录的累计正确率变化，用于观察趋势；学习画像中的掌握度为另一套动态模型，数值不完全等同。");
        return out;
    }

    private List<Long> resolveTopKpIds(Long userId) {
        List<Map<String, Object>> rows = learningRecordDao.selectTopKnowledgePointsByAttemptVolume(userId, TOP_KP_LINES);
        List<Long> ids = new ArrayList<>();
        if (rows == null) {
            return ids;
        }
        for (Map<String, Object> row : rows) {
            Long id = toLong(row.get("kpId"));
            if (id != null && id > 0) {
                ids.add(id);
            }
        }
        return ids;
    }

    private List<Map<String, Object>> buildOverallAccuracySeries(Long userId) {
        List<Map<String, Object>> buckets = learningRecordDao.selectDailyAnswerBucketsByUserId(userId);
        List<Map<String, Object>> series = new ArrayList<>();
        if (buckets == null || buckets.isEmpty()) {
            return series;
        }
        long cumCorrect = 0;
        long cumTotal = 0;
        for (Map<String, Object> row : buckets) {
            String day = formatDay(row.get("day"));
            if (day == null) {
                continue;
            }
            long dayCorrect = toLong(row.get("correctCount"));
            long dayTotal = toLong(row.get("totalCount"));
            cumCorrect += dayCorrect;
            cumTotal += dayTotal;
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", day);
            point.put("dailyAccuracy", ratioPercent(dayCorrect, dayTotal));
            point.put("cumulativeAccuracy", ratioPercent(cumCorrect, cumTotal));
            series.add(point);
        }
        return series;
    }

    private List<Map<String, Object>> buildKnowledgePointTrends(Long userId, List<Long> kpIds) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (kpIds.isEmpty()) {
            return result;
        }
        Map<Long, String> names = loadKpNames(kpIds);
        List<Map<String, Object>> timeline = learningRecordDao.selectKnowledgeAttemptTimeline(userId, kpIds);
        if (timeline == null || timeline.isEmpty()) {
            for (Long kpId : kpIds) {
                result.add(emptyKpTrend(kpId, names.get(kpId)));
            }
            return result;
        }

        TreeMap<String, List<Map<String, Object>>> byDay = new TreeMap<>();
        for (Map<String, Object> row : timeline) {
            String day = formatDay(row.get("day"));
            if (day == null) {
                continue;
            }
            byDay.computeIfAbsent(day, k -> new ArrayList<>()).add(row);
        }

        Map<Long, long[]> totals = new LinkedHashMap<>();
        for (Long kpId : kpIds) {
            totals.put(kpId, new long[]{0L, 0L});
        }

        Map<Long, List<Map<String, Object>>> seriesByKp = new LinkedHashMap<>();
        for (Long kpId : kpIds) {
            seriesByKp.put(kpId, new ArrayList<>());
        }

        for (String day : byDay.keySet()) {
            for (Map<String, Object> ev : byDay.get(day)) {
                Long kpId = toLong(ev.get("kpId"));
                if (kpId == null || !totals.containsKey(kpId)) {
                    continue;
                }
                boolean ok = toBool(ev.get("isCorrect"));
                long[] t = totals.get(kpId);
                t[1]++;
                if (ok) {
                    t[0]++;
                }
            }
            for (Long kpId : kpIds) {
                long[] t = totals.get(kpId);
                if (t[1] <= 0) {
                    continue;
                }
                Map<String, Object> pt = new LinkedHashMap<>();
                pt.put("date", day);
                pt.put("cumulativeAccuracy", ratioPercent(t[0], t[1]));
                seriesByKp.get(kpId).add(pt);
            }
        }

        for (Long kpId : kpIds) {
            Map<String, Object> block = new LinkedHashMap<>();
            block.put("kpId", kpId);
            block.put("kpName", Optional.ofNullable(names.get(kpId)).orElse("知识点 #" + kpId));
            block.put("series", seriesByKp.get(kpId));
            result.add(block);
        }
        return result;
    }

    private Map<String, Object> emptyKpTrend(Long kpId, String name) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("kpId", kpId);
        block.put("kpName", name != null ? name : "知识点 #" + kpId);
        block.put("series", Collections.emptyList());
        return block;
    }

    private Map<Long, String> loadKpNames(List<Long> kpIds) {
        Map<Long, String> map = new HashMap<>();
        for (Long kpId : kpIds) {
            KnowledgePoint kp = knowledgePointDao.selectById(kpId);
            if (kp != null && kp.getName() != null && !kp.getName().isEmpty()) {
                map.put(kpId, kp.getName());
            }
        }
        return map;
    }

    private static String formatDay(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof java.sql.Date) {
            return ((java.sql.Date) raw).toLocalDate().toString();
        }
        if (raw instanceof Date) {
            return Instant.ofEpochMilli(((Date) raw).getTime()).atZone(ZONE).toLocalDate().toString();
        }
        String s = raw.toString().trim();
        if (s.length() >= 10) {
            return s.substring(0, 10);
        }
        return s.isEmpty() ? null : s;
    }

    private static Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return Long.parseLong(o.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean toBool(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue() != 0;
        }
        String s = o.toString().trim();
        return "1".equals(s) || "true".equalsIgnoreCase(s);
    }

    /**
     * 返回 0–100，保留一位小数。
     */
    private static double ratioPercent(long correct, long total) {
        if (total <= 0) {
            return 0d;
        }
        return BigDecimal.valueOf(correct * 100.0 / total)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
