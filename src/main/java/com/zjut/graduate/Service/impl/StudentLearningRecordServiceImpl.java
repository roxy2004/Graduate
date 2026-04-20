package com.zjut.graduate.Service.impl;

import com.zjut.graduate.Dao.LearnerKnowledgeStateDao;
import com.zjut.graduate.Dao.LearningRecordDao;
import com.zjut.graduate.Dao.QuestionKnowledgePointRelDao;
import com.zjut.graduate.Po.LearnerKnowledgeState;
import com.zjut.graduate.Po.LearningRecord;
import com.zjut.graduate.Service.StudentLearningRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StudentLearningRecordServiceImpl implements StudentLearningRecordService {

    @Autowired
    private LearningRecordDao learningRecordDao;

    @Autowired
    private QuestionKnowledgePointRelDao questionKnowledgePointRelDao;

    @Autowired
    private LearnerKnowledgeStateDao learnerKnowledgeStateDao;

    @Override
    public List<Map<String, Object>> listRecords(Long userId) {
        return learningRecordDao.selectWithQuestionContentByUserId(userId);
    }

    @Override
    @Transactional
    public boolean deleteRecord(Long userId, Long recordId) {
        LearningRecord record = learningRecordDao.selectById(recordId);
        if (record == null || record.getUserId() == null || !record.getUserId().equals(userId)) {
            return false;
        }
        Long questionId = record.getQuestionId();
        Set<Long> affectedKpIds = new HashSet<>();
        if (questionId != null) {
            List<Long> kpIds = questionKnowledgePointRelDao.selectKpIdsByQuestionId(questionId);
            if (kpIds != null) {
                affectedKpIds.addAll(kpIds);
            }
        }
        boolean deleted = learningRecordDao.deleteByIdAndUserId(recordId, userId) > 0;
        if (!deleted) {
            return false;
        }
        recalculateStatesByKps(userId, affectedKpIds);
        return true;
    }

    @Override
    public int clearAll(Long userId) {
        return learningRecordDao.deleteAllByUserId(userId);
    }

    private void recalculateStatesByKps(Long userId, Set<Long> kpIds) {
        if (kpIds == null || kpIds.isEmpty()) {
            return;
        }
        List<Map<String, Object>> rows = learningRecordDao.selectKnowledgePracticeStatsByUserId(userId);
        Map<Long, Map<String, Object>> rowByKp = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object kid = row.get("kpId");
            if (kid instanceof Number) {
                rowByKp.put(((Number) kid).longValue(), row);
            }
        }
        Date now = new Date();
        for (Long kpId : kpIds) {
            if (kpId == null) continue;
            Map<String, Object> hit = rowByKp.get(kpId);
            int practiced = hit == null ? 0 : toInt(hit.get("practicedCount"));
            int correct = hit == null ? 0 : toInt(hit.get("correctCount"));
            Date lastAt = hit == null ? null : toDate(hit.get("lastPracticedAt"));

            double mastery;
            double confidence;
            if (practiced <= 0) {
                mastery = 0.50;
                confidence = 0.45;
                lastAt = null;
            } else {
                double acc = clamp01((double) correct / practiced);
                mastery = clamp01(0.35 + acc * 0.5);
                confidence = clamp(0.45 + 0.50 * Math.min(1.0, practiced / 40.0), 0.45, 0.95);
            }

            LearnerKnowledgeState state = learnerKnowledgeStateDao.selectByUserAndKp(userId, kpId);
            if (state == null) {
                LearnerKnowledgeState created = new LearnerKnowledgeState();
                created.setUserId(userId);
                created.setKpId(kpId);
                created.setMasteryLevel(round2(mastery));
                created.setConfidence(round2(confidence));
                created.setLastPracticedAt(lastAt);
                created.setUpdatedAt(now);
                learnerKnowledgeStateDao.insert(created);
            } else {
                state.setMasteryLevel(round2(mastery));
                state.setConfidence(round2(confidence));
                state.setLastPracticedAt(lastAt);
                state.setUpdatedAt(now);
                learnerKnowledgeStateDao.updateMastery(state);
            }
        }
    }

    private static int toInt(Object raw) {
        if (raw == null) return 0;
        if (raw instanceof Number) return ((Number) raw).intValue();
        try {
            return Integer.parseInt(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Date toDate(Object raw) {
        return raw instanceof Date ? (Date) raw : null;
    }

    private static double clamp01(double v) {
        return clamp(v, 0.0, 1.0);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double round2(double v) {
        return Math.round(v * 100D) / 100D;
    }
}

