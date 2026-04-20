package com.zjut.graduate.Service.impl;

import com.zjut.graduate.Dao.KnowledgePointDao;
import com.zjut.graduate.Dao.LearnerKnowledgeStateDao;
import com.zjut.graduate.Dao.LearningRecordDao;
import com.zjut.graduate.Dao.QuestionBankDao;
import com.zjut.graduate.Dao.QuestionKnowledgePointRelDao;
import com.zjut.graduate.Po.KnowledgePoint;
import com.zjut.graduate.Po.LearnerKnowledgeState;
import com.zjut.graduate.Po.LearningRecord;
import com.zjut.graduate.Po.QuestionBank;
import com.zjut.graduate.Service.LearningRouteService;
import com.zjut.graduate.Service.MistakeDeepAnalysisService;
import com.zjut.graduate.Service.StudentPracticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudentPracticeServiceImpl implements StudentPracticeService {

    @Autowired
    private KnowledgePointDao knowledgePointDao;

    @Autowired
    private QuestionBankDao questionBankDao;

    @Autowired
    private QuestionKnowledgePointRelDao questionKnowledgePointRelDao;

    @Autowired
    private LearningRecordDao learningRecordDao;

    @Autowired
    private LearnerKnowledgeStateDao learnerKnowledgeStateDao;

    @Autowired
    private MistakeDeepAnalysisService mistakeDeepAnalysisService;

    @Autowired
    private LearningRouteService learningRouteService;

    @Override
    public List<Map<String, Object>> listKnowledgePointPracticeSummary(Long userId) {
        return knowledgePointDao.selectPracticeSummaryByUser(userId);
    }

    @Override
    public Map<String, Object> getKnowledgePointPracticeSummary(Long userId, Long kpId) {
        KnowledgePoint kp = knowledgePointDao.selectById(kpId);
        if (kp == null) {
            return null;
        }
        List<Map<String, Object>> all = knowledgePointDao.selectPracticeSummaryByUser(userId);
        for (Map<String, Object> row : all) {
            Object id = row.get("id");
            if (id instanceof Number && ((Number) id).longValue() == kpId) {
                return row;
            }
        }
        Map<String, Object> empty = new LinkedHashMap<>();
        empty.put("id", kpId);
        empty.put("name", kp.getName());
        empty.put("totalQuestions", 0);
        empty.put("practicedQuestions", 0);
        return empty;
    }

    @Override
    public List<Map<String, Object>> getPracticeDeck(Long userId, Long kpId) {
        if (knowledgePointDao.selectById(kpId) == null) {
            return java.util.Collections.emptyList();
        }
        List<Map<String, Object>> raw = questionBankDao.selectPracticeDeckRows(kpId, userId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : raw) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", col(row, "id"));
            item.put("content", col(row, "content"));
            item.put("questionType", col(row, "questionType"));
            item.put("options", col(row, "options"));
            item.put("difficulty", col(row, "difficulty"));
            item.put("sourceTag", col(row, "sourceTag"));
            Object lua = col(row, "lastUserAnswer");
            Object lic = col(row, "lastIsCorrect");
            Object lat = col(row, "lastAnsweredAt");
            Object lts = col(row, "lastTimeSpent");
            Object ca = col(row, "correctAnswer");
            if (lua != null && !String.valueOf(lua).trim().isEmpty()) {
                Map<String, Object> la = new LinkedHashMap<>();
                la.put("userAnswer", String.valueOf(lua).trim().toUpperCase());
                boolean ok = false;
                if (lic instanceof Number) {
                    ok = ((Number) lic).intValue() == 1;
                } else if (lic instanceof Boolean) {
                    ok = (Boolean) lic;
                }
                la.put("isCorrect", ok);
                la.put("answeredAt", lat);
                Integer priorSec = null;
                if (lts instanceof Number) {
                    priorSec = Math.max(0, ((Number) lts).intValue());
                }
                la.put("timeSpent", priorSec);
                item.put("lastAttempt", la);
                item.put("correctAnswer", ca == null ? null : String.valueOf(ca).trim().toUpperCase());
            } else {
                item.put("lastAttempt", null);
                item.put("correctAnswer", null);
            }
            out.add(item);
        }
        return out;
    }

    @Override
    public List<Map<String, Object>> getRandomCrossKpPracticeDeck(Long userId, int limit) {
        int n = Math.max(1, Math.min(limit, 50));
        List<Map<String, Object>> raw = questionBankDao.selectRandomCrossKpPracticeDeckRows(userId, n);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : raw) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", col(row, "id"));
            item.put("content", col(row, "content"));
            item.put("questionType", col(row, "questionType"));
            item.put("options", col(row, "options"));
            item.put("difficulty", col(row, "difficulty"));
            item.put("sourceTag", col(row, "sourceTag"));
            Object kpid = col(row, "knowledgePointId");
            if (kpid instanceof Number) {
                item.put("knowledgePointId", ((Number) kpid).longValue());
            } else if (kpid != null) {
                try {
                    item.put("knowledgePointId", Long.parseLong(String.valueOf(kpid).trim()));
                } catch (NumberFormatException e) {
                    item.put("knowledgePointId", null);
                }
            } else {
                item.put("knowledgePointId", null);
            }
            Long kpIdForName = (Long) item.get("knowledgePointId");
            if (kpIdForName != null) {
                KnowledgePoint kp = knowledgePointDao.selectById(kpIdForName);
                item.put("knowledgePointName", kp != null && kp.getName() != null ? kp.getName() : "");
            } else {
                item.put("knowledgePointName", "");
            }
            Object lua = col(row, "lastUserAnswer");
            Object lic = col(row, "lastIsCorrect");
            Object lat = col(row, "lastAnsweredAt");
            Object lts = col(row, "lastTimeSpent");
            Object ca = col(row, "correctAnswer");
            if (lua != null && !String.valueOf(lua).trim().isEmpty()) {
                Map<String, Object> la = new LinkedHashMap<>();
                la.put("userAnswer", String.valueOf(lua).trim().toUpperCase());
                boolean ok = false;
                if (lic instanceof Number) {
                    ok = ((Number) lic).intValue() == 1;
                } else if (lic instanceof Boolean) {
                    ok = (Boolean) lic;
                }
                la.put("isCorrect", ok);
                la.put("answeredAt", lat);
                Integer priorSec = null;
                if (lts instanceof Number) {
                    priorSec = Math.max(0, ((Number) lts).intValue());
                }
                la.put("timeSpent", priorSec);
                item.put("lastAttempt", la);
                item.put("correctAnswer", ca == null ? null : String.valueOf(ca).trim().toUpperCase());
            } else {
                item.put("lastAttempt", null);
                item.put("correctAnswer", null);
            }
            out.add(item);
        }
        return out;
    }

    @Override
    public List<Map<String, Object>> getDailyRecommendedDeck(Long userId, int limit) {
        int n = Math.max(1, Math.min(limit, 50));
        Map<String, Object> payload = learningRouteService.getLatestRoute(userId, false);
        Object raw = payload == null ? null : payload.get("dailyQuestions");
        if (!(raw instanceof List)) {
            return java.util.Collections.emptyList();
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> daily = (List<Map<String, Object>>) raw;
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : daily) {
            if (out.size() >= n) break;
            Long qid = null;
            Object qraw = row.get("questionId");
            if (qraw instanceof Number) {
                qid = ((Number) qraw).longValue();
            } else if (qraw != null) {
                try {
                    qid = Long.parseLong(String.valueOf(qraw).trim());
                } catch (NumberFormatException ignore) {
                    qid = null;
                }
            }
            if (qid == null) continue;
            QuestionBank qb = questionBankDao.selectById(qid);
            if (qb == null || qb.getStatus() == null || qb.getStatus() != 1) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", qb.getId());
            item.put("content", qb.getContent());
            item.put("questionType", qb.getQuestionType());
            item.put("options", qb.getOptions());
            item.put("difficulty", qb.getDifficulty());
            item.put("sourceTag", qb.getSourceTag());
            item.put("knowledgePointId", row.get("knowledgePointId"));
            item.put("knowledgePointName", row.get("knowledgePointName"));
            item.put("doneToday", toBool(row.get("doneToday")));
            Map<String, Object> last = learningRecordDao.selectLatestAttemptByUserAndQuestion(userId, qid);
            if (last != null && last.get("userAnswer") != null && !String.valueOf(last.get("userAnswer")).trim().isEmpty()) {
                Map<String, Object> la = new LinkedHashMap<>();
                la.put("userAnswer", String.valueOf(last.get("userAnswer")).trim().toUpperCase());
                Object lic = last.get("isCorrect");
                boolean ok = false;
                if (lic instanceof Number) {
                    ok = ((Number) lic).intValue() == 1;
                } else if (lic instanceof Boolean) {
                    ok = (Boolean) lic;
                }
                la.put("isCorrect", ok);
                la.put("answeredAt", last.get("answeredAt"));
                Object lts = last.get("timeSpent");
                Integer priorSec = null;
                if (lts instanceof Number) {
                    priorSec = Math.max(0, ((Number) lts).intValue());
                }
                la.put("timeSpent", priorSec);
                item.put("lastAttempt", la);
                item.put("correctAnswer", qb.getCorrectAnswer() == null ? null : qb.getCorrectAnswer().trim().toUpperCase());
            } else {
                item.put("lastAttempt", null);
                item.put("correctAnswer", null);
            }
            out.add(item);
        }
        return out;
    }

    private static boolean toBool(Object raw) {
        if (raw instanceof Boolean) return (Boolean) raw;
        if (raw instanceof Number) return ((Number) raw).intValue() != 0;
        if (raw == null) return false;
        String s = String.valueOf(raw).trim();
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }

    private static Object col(Map<String, Object> row, String key) {
        if (row.containsKey(key)) {
            return row.get(key);
        }
        for (Map.Entry<String, Object> e : row.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
                return e.getValue();
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> nextQuestion(Long userId, Long kpId) {
        if (knowledgePointDao.selectById(kpId) == null) {
            return null;
        }
        QuestionBank qb = questionBankDao.selectNextPracticeQuestion(kpId, userId);
        if (qb == null) {
            return null;
        }
        return toPublicQuestion(qb);
    }

    @Override
    @Transactional
    public Map<String, Object> submitAttempt(Long userId, Long kpId, Long questionId, String userAnswer, Integer timeSpent) {
        Map<String, Object> out = new HashMap<>();
        if (questionId == null || questionKnowledgePointRelDao.countByQuestionAndKp(questionId, kpId) <= 0) {
            out.put("status", "error");
            out.put("message", "题目不属于该知识点");
            return out;
        }
        QuestionBank qb = questionBankDao.selectById(questionId);
        if (qb == null || qb.getStatus() == null || qb.getStatus() != 1) {
            out.put("status", "error");
            out.put("message", "题目不可用");
            return out;
        }
        String ua = userAnswer == null ? "" : userAnswer.trim().toUpperCase();
        if (ua.isEmpty()) {
            out.put("status", "error");
            out.put("message", "请选择答案");
            return out;
        }
        String correct = qb.getCorrectAnswer() == null ? "" : qb.getCorrectAnswer().trim();
        int isCorrect = correct.equalsIgnoreCase(ua) ? 1 : 0;
        int ts = timeSpent == null ? 0 : Math.max(0, Math.min(timeSpent, 3600));
        int attemptNo = learningRecordDao.selectMaxAttemptNo(userId, questionId) + 1;
        LearningRecord rec = new LearningRecord();
        rec.setUserId(userId);
        rec.setQuestionId(questionId);
        rec.setUserAnswer(ua);
        rec.setIsCorrect(isCorrect);
        rec.setTimeSpent(ts);
        rec.setAttemptNo(attemptNo);
        learningRecordDao.insertPracticeAttemptReturningId(rec);
        updateKnowledgeMastery(userId, questionId, isCorrect, qb.getDifficulty());
        if (isCorrect == 0 && rec.getId() != null) {
            final Long newRecordId = rec.getId();
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        mistakeDeepAnalysisService.analyzeAsync(newRecordId);
                    }
                });
            } else {
                mistakeDeepAnalysisService.analyzeAsync(newRecordId);
            }
        }
        out.put("status", "success");
        out.put("isCorrect", isCorrect == 1);
        out.put("correctAnswer", correct.toUpperCase());
        return out;
    }

    private void updateKnowledgeMastery(Long userId, Long questionId, int isCorrect, Double difficulty) {
        List<Long> kpIds = questionKnowledgePointRelDao.selectKpIdsByQuestionId(questionId);
        if (kpIds == null || kpIds.isEmpty()) {
            return;
        }
        Map<Long, Integer> practicedByKp = loadPracticedCountMap(userId);
        double score = isCorrect == 1 ? 1.0 : 0.0;
        Date now = new Date();

        for (Long kpId : kpIds) {
            if (kpId == null) {
                continue;
            }
            int practiced = practicedByKp.getOrDefault(kpId, 0);
            LearnerKnowledgeState state = learnerKnowledgeStateDao.selectByUserAndKp(userId, kpId);
            if (state == null) {
                LearnerKnowledgeState created = new LearnerKnowledgeState();
                created.setUserId(userId);
                created.setKpId(kpId);
                // 冷启动：以 0.50 为基线，首题允许较快收敛但不激进。
                created.setMasteryLevel(round2(applySmoothing(0.50, score, practiced, difficulty)));
                created.setConfidence(0.60);
                created.setLastPracticedAt(now);
                created.setUpdatedAt(now);
                learnerKnowledgeStateDao.insert(created);
                continue;
            }

            double oldMastery = state.getMasteryLevel() == null ? 0.5 : state.getMasteryLevel();
            double newMastery = applySmoothing(oldMastery, score, practiced, difficulty);
            double confidence = smoothConfidence(state.getConfidence(), practiced);

            state.setMasteryLevel(round2(newMastery));
            state.setConfidence(round2(confidence));
            state.setLastPracticedAt(now);
            state.setUpdatedAt(now);
            learnerKnowledgeStateDao.updateMastery(state);
        }
    }

    private Map<Long, Integer> loadPracticedCountMap(Long userId) {
        Map<Long, Integer> out = new HashMap<>();
        List<Map<String, Object>> rows = learningRecordDao.selectKnowledgePracticeStatsByUserId(userId);
        for (Map<String, Object> row : rows) {
            Object kid = row.get("kpId");
            if (!(kid instanceof Number)) {
                continue;
            }
            long kpId = ((Number) kid).longValue();
            Object c = row.get("practicedCount");
            int practiced = c instanceof Number ? ((Number) c).intValue() : 0;
            out.put(kpId, practiced);
        }
        return out;
    }

    /**
     * 掌握度平滑策略（增强版）：
     * 1) 自适应步长：练习越多，alpha 越小（避免成熟画像剧烈抖动）。
     * 2) 难度加权：难题影响略大、简单题略小。
     * 3) 单次限幅：限制每次更新的最大升降幅，抑制噪声。
     */
    private static double applySmoothing(double oldMastery, double score, int practiced, Double difficulty) {
        double old = clamp01(oldMastery);
        int p = Math.max(0, practiced);
        double diff = difficulty == null ? 0.5 : clamp01(difficulty);

        double baseAlpha;
        if (p < 8) {
            baseAlpha = 0.28;
        } else if (p < 30) {
            baseAlpha = 0.20;
        } else {
            baseAlpha = 0.14;
        }
        double difficultyFactor = 0.85 + 0.35 * diff; // 0.85 ~ 1.20
        double alpha = clamp(baseAlpha * difficultyFactor, 0.08, 0.35);

        double raw = old + alpha * (score - old);

        double maxStep;
        if (p < 10) {
            maxStep = 0.12;
        } else if (p < 30) {
            maxStep = 0.08;
        } else {
            maxStep = 0.05;
        }
        double bounded = clamp(raw, old - maxStep, old + maxStep);
        return clamp01(bounded);
    }

    private static double smoothConfidence(Double oldConfidence, int practiced) {
        int p = Math.max(0, practiced);
        double observed = 0.45 + 0.50 * Math.min(1.0, p / 40.0); // 0.45 ~ 0.95
        observed = clamp(observed, 0.45, 0.95);
        double old = oldConfidence == null ? observed : clamp(oldConfidence, 0.30, 0.95);
        return 0.7 * old + 0.3 * observed;
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

    @Override
    @Transactional
    public void clearPracticeRecords(Long userId, Long kpId) {
        if (knowledgePointDao.selectById(kpId) == null) {
            return;
        }
        learningRecordDao.deleteMistakeAnalysisByUserAndKp(userId, kpId);
        learningRecordDao.deleteLearningRecordsByUserAndKp(userId, kpId);
        // 删除记录后按剩余数据回退/重算画像，而不是直接清空。
        recalculateKnowledgeStateAfterRecordReset(userId, kpId);
    }

    /**
     * 清空知识点练习记录后的画像处理：
     * - 若该知识点仍有历史记录（兼容未来“部分删除”场景），按剩余统计重算；
     * - 若无记录，回退到冷启动基线（0.50）。
     */
    private void recalculateKnowledgeStateAfterRecordReset(Long userId, Long kpId) {
        List<Map<String, Object>> rows = learningRecordDao.selectKnowledgePracticeStatsByUserId(userId);
        Map<String, Object> hit = null;
        for (Map<String, Object> row : rows) {
            Object raw = row.get("kpId");
            if (raw instanceof Number && ((Number) raw).longValue() == kpId) {
                hit = row;
                break;
            }
        }

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
            mastery = clamp01(0.35 + acc * 0.5); // 与推荐兜底口径一致
            confidence = clamp(0.45 + 0.50 * Math.min(1.0, practiced / 40.0), 0.45, 0.95);
        }

        LearnerKnowledgeState state = learnerKnowledgeStateDao.selectByUserAndKp(userId, kpId);
        Date now = new Date();
        if (state == null) {
            LearnerKnowledgeState created = new LearnerKnowledgeState();
            created.setUserId(userId);
            created.setKpId(kpId);
            created.setMasteryLevel(round2(mastery));
            created.setConfidence(round2(confidence));
            created.setLastPracticedAt(lastAt);
            created.setUpdatedAt(now);
            learnerKnowledgeStateDao.insert(created);
            return;
        }
        state.setMasteryLevel(round2(mastery));
        state.setConfidence(round2(confidence));
        state.setLastPracticedAt(lastAt);
        state.setUpdatedAt(now);
        learnerKnowledgeStateDao.updateMastery(state);
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

    private Map<String, Object> toPublicQuestion(QuestionBank qb) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", qb.getId());
        m.put("content", qb.getContent());
        m.put("questionType", qb.getQuestionType());
        m.put("options", qb.getOptions());
        m.put("difficulty", qb.getDifficulty());
        m.put("sourceTag", qb.getSourceTag());
        m.put("knowledgePointIds", qb.getKnowledgePointIds());
        return m;
    }
}
