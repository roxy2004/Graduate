package com.zjut.graduate.Service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjut.graduate.Dao.DailyRecommendationSnapshotDao;
import com.zjut.graduate.Dao.KnowledgePointDao;
import com.zjut.graduate.Dao.LearnerKnowledgeStateDao;
import com.zjut.graduate.Dao.LearningRouteDao;
import com.zjut.graduate.Dao.LearningRecordDao;
import com.zjut.graduate.Dao.QuestionBankDao;
import com.zjut.graduate.Dao.QuestionKnowledgePointRelDao;
import com.zjut.graduate.Po.DailyRecommendationSnapshot;
import com.zjut.graduate.Po.KnowledgePoint;
import com.zjut.graduate.Po.LearnerKnowledgeState;
import com.zjut.graduate.Po.LearningRoute;
import com.zjut.graduate.Po.LearningRouteItem;
import com.zjut.graduate.Po.QuestionBank;
import com.zjut.graduate.Service.DeepSeekProxyService;
import com.zjut.graduate.Service.LearningRouteMaintenanceService;
import com.zjut.graduate.Service.LearningRouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class LearningRouteServiceImpl implements LearningRouteService {
    private static final String MODE_RULE = "rule_v4";
    private static final String MODE_AI = "ai_v4";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private LearningRouteDao learningRouteDao;

    @Autowired
    private LearningRouteMaintenanceService learningRouteMaintenanceService;

    @Autowired
    private LearnerKnowledgeStateDao learnerKnowledgeStateDao;

    @Autowired
    private LearningRecordDao learningRecordDao;

    @Autowired
    private KnowledgePointDao knowledgePointDao;

    @Autowired
    private QuestionBankDao questionBankDao;

    @Autowired
    private QuestionKnowledgePointRelDao questionKnowledgePointRelDao;

    @Autowired
    private DailyRecommendationSnapshotDao dailyRecommendationSnapshotDao;

    @Autowired(required = false)
    private DeepSeekProxyService deepSeekProxyService;

    @Override
    public Map<String, Object> getLatestRoute(Long userId) {
        return getLatestRoute(userId, true);
    }

    @Override
    public Map<String, Object> getLatestRoute(Long userId, boolean includeAi) {
        String mode = includeAi ? MODE_AI : MODE_RULE;
        Map<String, Object> cached = loadSnapshot(userId, mode);
        if (cached != null) {
            return attachTodayStatus(userId, cached, includeAi);
        }
        if (includeAi) {
            // 先确保规则快照存在，保证同一天题单一致。
            Map<String, Object> baseRule = loadSnapshot(userId, MODE_RULE);
            if (baseRule == null) {
                Map<String, Object> generatedRule = buildLatestRoutePayload(userId, false, null);
                saveSnapshot(userId, MODE_RULE, generatedRule);
                baseRule = generatedRule;
            }
            Map<String, Object> aiPayload = buildLatestRoutePayload(userId, true, baseRule);
            saveSnapshot(userId, MODE_AI, aiPayload);
            return attachTodayStatus(userId, aiPayload, true);
        }
        Map<String, Object> generated = buildLatestRoutePayload(userId, false, null);
        saveSnapshot(userId, MODE_RULE, generated);
        return attachTodayStatus(userId, generated, false);
    }

    private Map<String, Object> buildLatestRoutePayload(Long userId, boolean includeAi, Map<String, Object> baseRulePayload) {
        Map<String, Object> data = new HashMap<>();

        List<Map<String, Object>> personalized;
        List<Map<String, Object>> daily;
        boolean needRouteSync = false;
        if (baseRulePayload != null) {
            personalized = castMapList(baseRulePayload.get("personalized"));
            daily = castMapList(baseRulePayload.get("dailyQuestions"));
            if (personalized == null || personalized.isEmpty()) {
                personalized = buildPersonalizedRecommendations(userId);
                needRouteSync = true;
            }
            if (daily == null || daily.isEmpty()) {
                daily = buildDailyQuestions(userId, personalized, 10, false);
            }
        } else {
            personalized = buildPersonalizedRecommendations(userId);
            daily = buildDailyQuestions(userId, personalized, 10, false);
            needRouteSync = true;
        }
        if (needRouteSync) {
            learningRouteMaintenanceService.syncFromPersonalized(userId, personalized);
        }

        LearningRoute route = learningRouteDao.selectLatestByUserId(userId);
        List<Map<String, Object>> itemViews;
        if (route == null) {
            data.put("route", null);
            itemViews = new ArrayList<>();
        } else {
            List<LearningRouteItem> items = learningRouteDao.selectItemsByRouteId(route.getId());
            data.put("route", route);
            itemViews = enrichRouteItems(items, personalized, route);
        }
        data.put("items", itemViews);

        if (includeAi) {
            enrichDailyReasonsByAi(daily);
        }
        data.put("personalized", personalized);
        data.put("dailyQuestions", daily);
        data.put("prediction", buildPrediction(userId, personalized));
        data.put("aiInsights", includeAi ? buildAiInsights(personalized, daily, data.get("prediction")) : buildAiDisabledHint());
        return data;
    }

    private List<Map<String, Object>> enrichRouteItems(List<LearningRouteItem> items, List<Map<String, Object>> personalized,
                                                       LearningRoute route) {
        Map<Long, Map<String, Object>> byKp = new HashMap<>();
        if (personalized != null) {
            for (Map<String, Object> p : personalized) {
                Long id = toLong(p.get("kpId"));
                if (id != null) {
                    byKp.put(id, p);
                }
            }
        }
        boolean ruleEngineRoute = route != null && route.getGeneratedBy() != null
                && "rule_engine".equals(route.getGeneratedBy());
        List<Map<String, Object>> out = new ArrayList<>();
        if (items == null) {
            return out;
        }
        for (LearningRouteItem it : items) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", it.getId());
            m.put("routeId", it.getRouteId());
            m.put("itemType", it.getItemType());
            m.put("itemId", it.getItemId());
            m.put("reason", it.getReason());
            m.put("priority", it.getPriority());
            m.put("estimatedMinutes", it.getEstimatedMinutes());
            m.put("sortNo", it.getSortNo());
            m.put("completed", it.getCompleted());
            Long kpId = it.getItemId();
            Map<String, Object> p = kpId == null ? null : byKp.get(kpId);
            if (kpId != null && p != null && ruleEngineRoute) {
                m.put("kpName", p.get("kpName"));
                m.put("masteryPercent", p.get("masteryPercent"));
                m.put("accuracyPercent", p.get("accuracyPercent"));
                m.put("remainingQuestions", p.get("remainingQuestions"));
                m.put("actionPath", "/manager/student/practice/kp/" + kpId);
            } else if (kpId != null && isKnowledgePointStepType(it.getItemType())) {
                if (p != null) {
                    m.put("kpName", p.get("kpName"));
                    m.put("masteryPercent", p.get("masteryPercent"));
                    m.put("accuracyPercent", p.get("accuracyPercent"));
                    m.put("remainingQuestions", p.get("remainingQuestions"));
                }
                m.put("actionPath", "/manager/student/practice/kp/" + kpId);
            }
            out.add(m);
        }
        return out;
    }

    private static boolean isKnowledgePointStepType(String itemType) {
        if (itemType == null) {
            return false;
        }
        String t = itemType.trim();
        return "KNOWLEDGE_POINT".equalsIgnoreCase(t) || "knowledge_point".equalsIgnoreCase(t);
    }

    private Map<String, Object> loadSnapshot(Long userId, String mode) {
        DailyRecommendationSnapshot snapshot = dailyRecommendationSnapshotDao.selectTodayByUserAndMode(userId, mode);
        if (snapshot == null || snapshot.getPayloadJson() == null || snapshot.getPayloadJson().trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(snapshot.getPayloadJson(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private void saveSnapshot(Long userId, String mode, Map<String, Object> payload) {
        if (payload == null) return;
        try {
            String json = objectMapper.writeValueAsString(payload);
            DailyRecommendationSnapshot existing = dailyRecommendationSnapshotDao.selectTodayByUserAndMode(userId, mode);
            if (existing == null) {
                DailyRecommendationSnapshot s = new DailyRecommendationSnapshot();
                s.setUserId(userId);
                s.setMode(mode);
                s.setPayloadJson(json);
                dailyRecommendationSnapshotDao.insert(s);
            } else {
                dailyRecommendationSnapshotDao.updatePayloadById(existing.getId(), json);
            }
        } catch (Exception e) {
            // ignore snapshot failures, keep online generation path available.
        }
    }

    private Map<String, Object> attachTodayStatus(Long userId, Map<String, Object> payload, boolean includeAi) {
        Map<String, Object> out = deepCopy(payload);
        List<Map<String, Object>> daily = castMapList(out.get("dailyQuestions"));
        if (daily != null) {
            markDoneToday(userId, daily);
            out.put("dailyQuestions", daily);
        }
        refreshRealtimePanels(userId, out, includeAi, daily);
        attachRouteProgress(userId, out);
        return out;
    }

    /**
     * 每日题单来自快照（保证当天固定），其余面板实时刷新：
     * - personalized（知识点优先级）
     * - prediction（当前正确率/预测正确率）
     * - route/items（基于实时 personalized 同步与回读）
     */
    private void refreshRealtimePanels(Long userId,
                                       Map<String, Object> payload,
                                       boolean includeAi,
                                       List<Map<String, Object>> daily) {
        List<Map<String, Object>> personalized = buildPersonalizedRecommendations(userId);
        payload.put("personalized", personalized);
        payload.put("prediction", buildPrediction(userId, personalized));

        learningRouteMaintenanceService.syncFromPersonalized(userId, personalized);
        LearningRoute route = learningRouteDao.selectLatestByUserId(userId);
        if (route == null) {
            payload.put("route", null);
            payload.put("items", new ArrayList<>());
        } else {
            List<LearningRouteItem> items = learningRouteDao.selectItemsByRouteId(route.getId());
            payload.put("route", route);
            payload.put("items", enrichRouteItems(items, personalized, route));
        }

        if (!includeAi) {
            payload.put("aiInsights", buildAiDisabledHint());
            return;
        }
        Object rawAi = payload.get("aiInsights");
        if (!(rawAi instanceof Map)) {
            payload.put("aiInsights", buildAiInsights(personalized, daily == null ? new ArrayList<>() : daily, payload.get("prediction")));
        }
    }

    /**
     * 根据最新掌握度刷新路线步骤完成状态（仅影响返回数据，不写回 item 表）。
     */
    private void attachRouteProgress(Long userId, Map<String, Object> payload) {
        Object rawItems = payload.get("items");
        if (!(rawItems instanceof List)) {
            return;
        }
        List<?> list = (List<?>) rawItems;
        if (list.isEmpty()) {
            return;
        }
        List<LearnerKnowledgeState> states = learnerKnowledgeStateDao.selectByUserId(userId);
        Map<Long, Double> masteryByKp = new HashMap<>();
        for (LearnerKnowledgeState st : states) {
            if (st != null && st.getKpId() != null && st.getMasteryLevel() != null) {
                masteryByKp.put(st.getKpId(), clamp01(st.getMasteryLevel()));
            }
        }
        boolean ruleEngineRoute = false;
        Object routeObj = payload.get("route");
        if (routeObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> rm = (Map<String, Object>) routeObj;
            Object gb = rm.get("generatedBy");
            ruleEngineRoute = "rule_engine".equals(String.valueOf(gb));
        }
        for (Object o : list) {
            if (!(o instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) o;
            String type = m.get("itemType") == null ? "" : String.valueOf(m.get("itemType"));
            if (!ruleEngineRoute && !isKnowledgePointStepType(type)) {
                continue;
            }
            Long kpId = toLong(m.get("itemId"));
            if (kpId == null) {
                continue;
            }
            Double mast = masteryByKp.get(kpId);
            if (mast != null) {
                m.put("masteryPercent", round1(mast * 100D));
                m.put("completed", mast >= 0.75 ? 1 : 0);
            }
        }
    }

    private Map<String, Object> deepCopy(Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>(payload);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castMapList(Object raw) {
        if (!(raw instanceof List)) return null;
        List<?> list = (List<?>) raw;
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Map) {
                out.add(new LinkedHashMap<>((Map<String, Object>) o));
            }
        }
        return out;
    }

    private List<Map<String, Object>> buildDailyQuestions(Long userId, List<Map<String, Object>> personalized, int targetSize, boolean includeAi) {
        int limit = Math.max(1, targetSize);
        int minRedoQuota = Math.min(2, limit);
        int maxRedoQuota = Math.min(3, limit);
        int minUnseenQuota = Math.min(5, limit);
        List<Map<String, Object>> out = new ArrayList<>();
        Set<Long> addedQids = new HashSet<>();
        Map<Long, List<Map<String, Object>>> poolByKp = new LinkedHashMap<>();
        Map<Long, String> kpNameMap = new HashMap<>();
        Map<Long, String> kpReasonMap = new HashMap<>();

        for (Map<String, Object> rec : personalized) {
            Long kpId = toLong(rec.get("kpId"));
            if (kpId == null) continue;
            String kpName = String.valueOf(rec.getOrDefault("kpName", "该知识点"));
            String kpReason = String.valueOf(rec.getOrDefault("reason", "该知识点建议优先巩固"));
            List<Map<String, Object>> candidates = questionBankDao.selectDailyCandidatesByKp(userId, kpId, Math.max(30, limit * 3));
            if (candidates == null || candidates.isEmpty()) continue;
            poolByKp.put(kpId, new ArrayList<>(candidates));
            kpNameMap.put(kpId, kpName);
            kpReasonMap.put(kpId, kpReason);
        }

        // 配额控制：先尽量选满至少 2 题错题重做。
        int redoPicked = 0;
        int unseenPicked = 0;
        while (out.size() < limit && redoPicked < minRedoQuota && !poolByKp.isEmpty()) {
            boolean pickedInRound = false;
            List<Long> kpIds = new ArrayList<>(poolByKp.keySet());
            for (Long kpId : kpIds) {
                if (out.size() >= limit || redoPicked >= minRedoQuota) break;
                List<Map<String, Object>> queue = poolByKp.getOrDefault(kpId, Collections.emptyList());
                Map<String, Object> selected = pickRedoCandidate(queue, addedQids);
                if (selected == null) {
                    continue;
                }
                Long qid = toLong(selected.get("id"));
                if (qid == null || addedQids.contains(qid)) continue;
                out.add(toDailyItem(selected, kpId, kpNameMap.get(kpId), kpReasonMap.get(kpId), qid));
                addedQids.add(qid);
                if (toInt(selected.get("attemptedCount")) <= 0) {
                    unseenPicked++;
                }
                queue.remove(selected);
                if (queue.isEmpty()) {
                    poolByKp.remove(kpId);
                }
                redoPicked++;
                pickedInRound = true;
            }
            if (!pickedInRound) break;
        }
        redoPicked = ensureRedoQuotaFromMistakes(userId, out, addedQids, redoPicked, minRedoQuota, limit);

        // 第一轮：知识点轮询，尽量覆盖不同知识点。
        while (out.size() < limit && !poolByKp.isEmpty()) {
            boolean pickedInRound = false;
            List<Long> kpIds = new ArrayList<>(poolByKp.keySet());
            for (Long kpId : kpIds) {
                if (out.size() >= limit) break;
                List<Map<String, Object>> queue = poolByKp.getOrDefault(kpId, Collections.emptyList());
                Map<String, Object> selected = pickBestCandidate(
                        queue, addedQids, redoPicked, maxRedoQuota, unseenPicked, minUnseenQuota, out.size(), limit
                );
                if (selected == null) {
                    poolByKp.remove(kpId);
                    continue;
                }
                Long qid = toLong(selected.get("id"));
                if (qid == null || addedQids.contains(qid)) {
                    continue;
                }
                out.add(toDailyItem(selected, kpId, kpNameMap.get(kpId), kpReasonMap.get(kpId), qid));
                addedQids.add(qid);
                if (toInt(selected.get("wrongCount")) > 0) {
                    redoPicked++;
                }
                if (toInt(selected.get("attemptedCount")) <= 0) {
                    unseenPicked++;
                }
                queue.remove(selected);
                if (queue.isEmpty()) {
                    poolByKp.remove(kpId);
                }
                pickedInRound = true;
            }
            if (!pickedInRound) break;
        }

        markDoneToday(userId, out);
        for (Map<String, Object> d : out) {
            d.put("reasonBy", "rule");
        }
        return out;
    }

    private int ensureRedoQuotaFromMistakes(Long userId,
                                            List<Map<String, Object>> out,
                                            Set<Long> addedQids,
                                            int redoPicked,
                                            int minRedoQuota,
                                            int limit) {
        if (redoPicked >= minRedoQuota || out.size() >= limit) return redoPicked;
        List<Map<String, Object>> mistakes = learningRecordDao.selectMistakesByUserId(userId);
        for (Map<String, Object> m : mistakes) {
            if (redoPicked >= minRedoQuota || out.size() >= limit) break;
            Long qid = toLong(m.get("question_id"));
            if (qid == null || addedQids.contains(qid)) continue;
            QuestionBank qb = questionBankDao.selectById(qid);
            if (qb == null || qb.getStatus() == null || qb.getStatus() != 1) continue;
            Long kpId = questionKnowledgePointRelDao.selectFirstKpIdByQuestionId(qid);
            KnowledgePoint kp = kpId == null ? null : knowledgePointDao.selectById(kpId);
            String kpName = kp == null || kp.getName() == null ? "综合知识点" : kp.getName();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("questionId", qid);
            item.put("content", qb.getContent());
            item.put("questionType", qb.getQuestionType());
            item.put("difficulty", qb.getDifficulty());
            item.put("sourceTag", qb.getSourceTag());
            item.put("knowledgePointId", kpId);
            item.put("knowledgePointName", kpName);
            item.put("attemptedCount", 1);
            item.put("wrongCount", 1);
            item.put("isRedo", true);
            item.put("reason", "错题重做：该题历史作答存在错误，建议优先复盘。");
            item.put("actionPath", kpId == null
                    ? ("/manager/student/practice/random?qid=" + qid)
                    : ("/manager/student/practice/kp/" + kpId + "?qid=" + qid));
            out.add(item);
            addedQids.add(qid);
            redoPicked++;
        }
        return redoPicked;
    }

    private Map<String, Object> pickBestCandidate(List<Map<String, Object>> queue, Set<Long> addedQids,
                                                  int redoPicked, int maxRedoQuota,
                                                  int unseenPicked, int minUnseenQuota,
                                                  int currentSize, int limit) {
        if (queue == null || queue.isEmpty()) return null;
        // 优先错题重做（但受最大上限约束）；若没有则按稳定顺序挑题，保证当天题单不因“已做”而被替换。
        Map<String, Object> redo = null;
        Map<String, Object> firstAny = null;
        Map<String, Object> firstNonWrong = null;
        Map<String, Object> firstUnseen = null;
        for (Map<String, Object> q : queue) {
            Long qid = toLong(q.get("id"));
            if (qid == null || addedQids.contains(qid)) continue;
            int wrong = toInt(q.get("wrongCount"));
            int attempted = toInt(q.get("attemptedCount"));
            if (wrong > 0 && redo == null && redoPicked < maxRedoQuota) {
                redo = q;
                continue;
            }
            if (firstAny == null) firstAny = q;
            if (wrong <= 0 && firstNonWrong == null) {
                firstNonWrong = q;
            }
            if (attempted <= 0 && firstUnseen == null) {
                firstUnseen = q;
            }
        }
        int remainSlots = Math.max(0, limit - currentSize);
        int unseenNeed = Math.max(0, minUnseenQuota - unseenPicked);
        if (unseenNeed > 0 && remainSlots <= unseenNeed && firstUnseen != null) {
            return firstUnseen;
        }
        if (redo != null) return redo;
        if (unseenNeed > 0 && firstUnseen != null) return firstUnseen;
        if (redoPicked >= maxRedoQuota && firstNonWrong != null) return firstNonWrong;
        return firstAny;
    }

    private Map<String, Object> pickRedoCandidate(List<Map<String, Object>> queue, Set<Long> addedQids) {
        if (queue == null || queue.isEmpty()) return null;
        for (Map<String, Object> q : queue) {
            Long qid = toLong(q.get("id"));
            if (qid == null || addedQids.contains(qid)) continue;
            int attempted = toInt(q.get("attemptedCount"));
            int wrong = toInt(q.get("wrongCount"));
            if (attempted > 0 && wrong > 0) {
                return q;
            }
        }
        return null;
    }

    private Map<String, Object> toDailyItem(Map<String, Object> q, Long kpId, String kpName, String kpReason, Long qid) {
        int attempted = toInt(q.get("attemptedCount"));
        int wrong = toInt(q.get("wrongCount"));
        boolean isRedo = attempted > 0;
        String reason;
        if (attempted <= 0) {
            reason = "来自「" + kpName + "」：未做过且该知识点优先级较高；" + kpReason;
        } else if (wrong > 0) {
            reason = "来自「" + kpName + "」：错题重做（历史错误 " + wrong + " 次），建议巩固纠错；" + kpReason;
        } else {
            reason = "来自「" + kpName + "」：已做过题目复盘，强化稳定性；" + kpReason;
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("questionId", qid);
        item.put("content", q.get("content"));
        item.put("questionType", q.get("questionType"));
        item.put("difficulty", q.get("difficulty"));
        item.put("sourceTag", q.get("sourceTag"));
        item.put("knowledgePointId", kpId);
        item.put("knowledgePointName", kpName);
        item.put("attemptedCount", attempted);
        item.put("wrongCount", wrong);
        item.put("isRedo", isRedo);
        item.put("doneToday", false);
        item.put("reason", reason);
        item.put("actionPath", "/manager/student/practice/kp/" + kpId + "?qid=" + qid);
        return item;
    }

    private void markDoneToday(Long userId, List<Map<String, Object>> out) {
        for (Map<String, Object> item : out) {
            Long qid = toLong(item.get("questionId"));
            if (qid == null) {
                item.put("doneToday", false);
                continue;
            }
            int c = learningRecordDao.countTodayAttemptsByUserAndQuestion(userId, qid);
            item.put("doneToday", c > 0);
        }
    }

    private List<Map<String, Object>> buildPersonalizedRecommendations(Long userId) {
        List<Map<String, Object>> practiceSummary = knowledgePointDao.selectPracticeSummaryByUser(userId);
        List<LearnerKnowledgeState> states = learnerKnowledgeStateDao.selectByUserId(userId);
        List<Map<String, Object>> kpStats = learningRecordDao.selectKnowledgePracticeStatsByUserId(userId);
        List<Map<String, Object>> timeRatios = knowledgePointDao.selectLearningTimeRatiosByUser(userId);

        Map<Long, LearnerKnowledgeState> stateByKp = new HashMap<>();
        for (LearnerKnowledgeState st : states) {
            if (st != null && st.getKpId() != null) {
                stateByKp.put(st.getKpId(), st);
            }
        }
        Map<Long, Map<String, Object>> statsByKp = new HashMap<>();
        for (Map<String, Object> row : kpStats) {
            Long kpId = toLong(row.get("kpId"));
            if (kpId != null) {
                statsByKp.put(kpId, row);
            }
        }
        Map<Long, Map<String, Object>> timeByKp = new HashMap<>();
        for (Map<String, Object> row : timeRatios) {
            Long kpId = toLong(row.get("kpId"));
            if (kpId != null) {
                timeByKp.put(kpId, row);
            }
        }

        List<Map<String, Object>> scored = new ArrayList<>();
        for (Map<String, Object> row : practiceSummary) {
            Long kpId = toLong(row.get("id"));
            if (kpId == null) continue;
            String kpName = row.get("name") == null ? ("知识点#" + kpId) : String.valueOf(row.get("name"));
            int total = toInt(row.get("totalQuestions"));
            int practiced = toInt(row.get("practicedQuestions"));
            if (total <= 0) continue;

            LearnerKnowledgeState st = stateByKp.get(kpId);
            Map<String, Object> stat = statsByKp.get(kpId);
            Map<String, Object> time = timeByKp.get(kpId);
            int attempted = toInt(stat == null ? null : stat.get("practicedCount"));
            int correct = toInt(stat == null ? null : stat.get("correctCount"));
            double mastery = st == null || st.getMasteryLevel() == null ? fallbackMastery(practiced, correct) : clamp01(st.getMasteryLevel());
            double confidence = st == null || st.getConfidence() == null ? 0.55 : clamp01(st.getConfidence());
            // 口径统一为“作答次数正确率”；避免 distinct 题目数作分母导致 >100%。
            double accuracy = attempted <= 0 ? 0.5 : clamp01((double) correct / attempted);
            double forgetting = recencyPenalty(st == null ? null : st.getLastPracticedAt());
            double novelty = practiced <= 2 ? 0.15 : practiced <= 5 ? 0.08 : 0.0;
            double timeRatio = Math.max(0.0, Math.min(1.5, toDouble(time == null ? null : time.get("timeRatio"))));
            // 只对“时长未达标”加压，达标后不再继续加压（避免刷时长影响推荐）。
            double timeGap = Math.max(0.0, 1.0 - clamp01(timeRatio));

            double weakScore = (1.0 - mastery) * 0.45;
            double errorScore = (1.0 - accuracy) * 0.25;
            double confScore = (1.0 - confidence) * 0.18;
            double forgetScore = forgetting * 0.12;
            double timeScore = timeGap * 0.10;
            double priority = clamp01(weakScore + errorScore + confScore + forgetScore + novelty + timeScore);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("kpId", kpId);
            item.put("kpName", kpName);
            item.put("priorityScore", round3(priority));
            item.put("mastery", round3(mastery));
            item.put("masteryPercent", round1(mastery * 100D));
            item.put("accuracyPercent", round1(accuracy * 100D));
            item.put("confidencePercent", round1(confidence * 100D));
            item.put("practicedCount", practiced);
            item.put("remainingQuestions", Math.max(0, total - practiced));
            item.put("timeRatioPercent", round1(clamp01(timeRatio) * 100D));
            item.put("timeGapPercent", round1(timeGap * 100D));
            item.put("reason", buildReason(mastery, accuracy, confidence, forgetting, practiced));
            item.put("actionType", "practice");
            item.put("actionPath", "/manager/student/practice/kp/" + kpId);
            scored.add(item);
        }
        scored.sort(Comparator.comparingDouble(o -> -toDouble(o.get("priorityScore"))));
        if (scored.size() > 5) {
            return new ArrayList<>(scored.subList(0, 5));
        }
        return scored;
    }

    private Map<String, Object> buildPrediction(Long userId, List<Map<String, Object>> recs) {
        int solvedCount = learningRecordDao.countByUserId(userId);
        int correctCount = learningRecordDao.countCorrectByUserId(userId);
        double currentAccuracy = solvedCount <= 0 ? 0.0 : (double) correctCount / solvedCount;

        List<LearnerKnowledgeState> states = learnerKnowledgeStateDao.selectByUserId(userId);
        double avgMastery = 0.5;
        if (!states.isEmpty()) {
            double sum = 0.0;
            int n = 0;
            for (LearnerKnowledgeState st : states) {
                if (st != null && st.getMasteryLevel() != null) {
                    sum += clamp01(st.getMasteryLevel());
                    n++;
                }
            }
            if (n > 0) avgMastery = sum / n;
        }

        double recommendBoost = 0.0;
        int boostCount = Math.min(3, recs.size());
        for (int i = 0; i < boostCount; i++) {
            double mastery = toDouble(recs.get(i).get("mastery"));
            recommendBoost += (1.0 - mastery) * 0.04;
        }
        recommendBoost = Math.min(0.10, recommendBoost);

        double blended = currentAccuracy * 0.55 + avgMastery * 0.45 + recommendBoost;
        double predictedAccuracy = clamp01(blended);
        int horizon = 10;
        int predictedCorrectNext = (int) Math.round(predictedAccuracy * horizon);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("currentAccuracyPercent", round1(currentAccuracy * 100D));
        out.put("predictedAccuracyPercent", round1(predictedAccuracy * 100D));
        out.put("predictedCorrectNext10", predictedCorrectNext);
        out.put("predictedMasteryGainPercent", round1(recommendBoost * 100D));
        out.put("confidenceLabel", confidenceLabel(solvedCount, states.size()));
        out.put("model", "rule-based-v1");
        out.put("horizon", horizon);
        return out;
    }

    private static String buildReason(double mastery, double accuracy, double confidence, double forgetting, int practiced) {
        if (practiced <= 2) return "练习样本较少，先完成基础题建立稳定画像";
        if (mastery < 0.45) return "当前掌握度偏低，优先巩固可快速补齐短板";
        if (accuracy < 0.6) return "近期正确率偏低，建议集中练习纠错";
        if (forgetting > 0.7) return "距离上次练习较久，建议复习防止遗忘";
        if (confidence < 0.55) return "画像置信度一般，补充练习可提升评估稳定性";
        return "建议常规巩固，保持当前掌握水平";
    }

    private static String confidenceLabel(int solvedCount, int kpCount) {
        int scale = Math.min(100, solvedCount + kpCount * 2);
        if (scale >= 60) return "高";
        if (scale >= 25) return "中";
        return "低";
    }

    private static double recencyPenalty(Date lastPracticedAt) {
        if (lastPracticedAt == null) return 0.8;
        long gapMs = System.currentTimeMillis() - lastPracticedAt.getTime();
        long days = Math.max(0, TimeUnit.MILLISECONDS.toDays(gapMs));
        if (days >= 14) return 1.0;
        if (days >= 7) return 0.75;
        if (days >= 3) return 0.45;
        return 0.10;
    }

    private static double fallbackMastery(int practiced, int correct) {
        if (practiced <= 0) return 0.5;
        double acc = (double) correct / practiced;
        return clamp01(0.35 + acc * 0.5);
    }

    private static Long toLong(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Number) return ((Number) raw).longValue();
        try {
            return Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            return null;
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

    private static double toDouble(Object raw) {
        if (raw == null) return 0D;
        if (raw instanceof Number) return ((Number) raw).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            return 0D;
        }
    }

    private static double clamp01(double v) {
        return Math.max(0D, Math.min(1D, v));
    }

    private static double round1(double v) {
        return Math.round(v * 10D) / 10D;
    }

    private static double round3(double v) {
        return Math.round(v * 1000D) / 1000D;
    }

    private Map<String, Object> buildAiDisabledHint() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", false);
        out.put("summary", "当前为快速模式，已跳过 AI 生成以提升加载速度。");
        out.put("predictionComment", "进入“预测推荐”页会启用 AI 增强说明。");
        return out;
    }

    private Map<String, Object> buildAiInsights(List<Map<String, Object>> personalized,
                                                List<Map<String, Object>> daily,
                                                Object predictionObj) {
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("enabled", false);
        fallback.put("summary", "当前为规则引擎推荐结果。配置 DeepSeek 后将生成更细粒度的学习建议。");
        fallback.put("predictionComment", "预测值基于掌握度与正确率加权估算。");
        if (deepSeekProxyService == null || !deepSeekProxyService.isConfigured()) {
            return fallback;
        }
        try {
            StringBuilder recSb = new StringBuilder();
            for (int i = 0; i < Math.min(3, personalized.size()); i++) {
                Map<String, Object> r = personalized.get(i);
                recSb.append(i + 1).append(". ")
                        .append(String.valueOf(r.getOrDefault("kpName", "知识点")))
                        .append(" 掌握度=").append(r.getOrDefault("masteryPercent", 0)).append("%")
                        .append(" 正确率=").append(r.getOrDefault("accuracyPercent", 0)).append("%; ");
            }
            StringBuilder dailySb = new StringBuilder();
            for (int i = 0; i < Math.min(5, daily.size()); i++) {
                Map<String, Object> d = daily.get(i);
                dailySb.append(i + 1).append(". ")
                        .append(d.getOrDefault("knowledgePointName", "知识点"))
                        .append(" - ")
                        .append(Boolean.TRUE.equals(d.get("isRedo")) ? "已做过" : "未做过")
                        .append(";");
            }
            String prediction = String.valueOf(predictionObj);
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> system = new LinkedHashMap<>();
            system.put("role", "system");
            system.put("content", "你是学习策略顾问。请输出简洁中文，不超过120字，给出两句话：1) 今日学习总建议 2) 对预测结果的解释。");
            messages.add(system);
            Map<String, String> user = new LinkedHashMap<>();
            user.put("role", "user");
            user.put("content",
                    "推荐Top知识点: " + recSb +
                            "每日题概览: " + dailySb +
                            "预测数据: " + prediction +
                            "。请输出JSON: {\"summary\":\"...\",\"predictionComment\":\"...\"}");
            messages.add(user);
            String raw = deepSeekProxyService.chatDirect(messages, 0.4);
            Map<String, String> parsed = parseSimpleJson(raw);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("enabled", true);
            out.put("summary", parsed.getOrDefault("summary", "已启用AI增强建议。"));
            out.put("predictionComment", parsed.getOrDefault("predictionComment", "预测结果由近期表现与掌握度共同决定。"));
            return out;
        } catch (Exception e) {
            return fallback;
        }
    }

    private void enrichDailyReasonsByAi(List<Map<String, Object>> daily) {
        if (daily == null || daily.isEmpty()) return;
        if (deepSeekProxyService == null || !deepSeekProxyService.isConfigured()) return;
        try {
            StringBuilder items = new StringBuilder();
            for (Map<String, Object> d : daily) {
                Long qid = toLong(d.get("questionId"));
                String kp = String.valueOf(d.getOrDefault("knowledgePointName", "知识点"));
                boolean redo = Boolean.TRUE.equals(d.get("isRedo"));
                int wrong = toInt(d.get("wrongCount"));
                String stem = String.valueOf(d.getOrDefault("content", ""));
                if (stem.length() > 80) {
                    stem = stem.substring(0, 80);
                }
                items.append("qid=").append(qid)
                        .append(",kp=").append(kp)
                        .append(",redo=").append(redo)
                        .append(",wrong=").append(wrong)
                        .append(",stem=").append(stem)
                        .append("\n");
            }
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> system = new LinkedHashMap<>();
            system.put("role", "system");
            system.put("content", "你是学习路径推荐助手。请为每道题生成一句推荐理由（15-35字），强调薄弱点、复习或新题探索。严格返回JSON对象，键为qid字符串，值为理由。不要输出任何额外文本。");
            messages.add(system);
            Map<String, String> user = new LinkedHashMap<>();
            user.put("role", "user");
            user.put("content", "请为以下题目生成理由：\n" + items);
            messages.add(user);
            String raw = deepSeekProxyService.chatDirect(messages, 0.4);
            for (Map<String, Object> d : daily) {
                Long qid = toLong(d.get("questionId"));
                if (qid == null) continue;
                String aiReason = extractJsonField(raw, String.valueOf(qid));
                if (aiReason != null && !aiReason.trim().isEmpty()) {
                    d.put("reason", aiReason.trim());
                    d.put("reasonBy", "ai");
                } else {
                    d.put("reasonBy", "rule");
                }
            }
        } catch (Exception e) {
            for (Map<String, Object> d : daily) {
                d.put("reasonBy", "rule");
            }
        }
    }

    private static Map<String, String> parseSimpleJson(String raw) {
        Map<String, String> out = new HashMap<>();
        if (raw == null) return out;
        String text = raw.trim();
        // 兼容模型多余说明，粗提取两个字段。
        String summary = extractJsonField(text, "summary");
        String predictionComment = extractJsonField(text, "predictionComment");
        if (summary != null && !summary.isEmpty()) out.put("summary", summary);
        if (predictionComment != null && !predictionComment.isEmpty()) out.put("predictionComment", predictionComment);
        return out;
    }

    private static String extractJsonField(String src, String key) {
        String token = "\"" + key + "\"";
        int i = src.indexOf(token);
        if (i < 0) return "";
        int colon = src.indexOf(':', i + token.length());
        if (colon < 0) return "";
        int firstQuote = src.indexOf('"', colon + 1);
        if (firstQuote < 0) return "";
        int end = src.indexOf('"', firstQuote + 1);
        if (end < 0) return "";
        return src.substring(firstQuote + 1, end).trim();
    }
}

