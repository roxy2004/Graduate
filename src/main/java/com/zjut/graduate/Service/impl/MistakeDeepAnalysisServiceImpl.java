package com.zjut.graduate.Service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjut.graduate.Dao.KnowledgePointDao;
import com.zjut.graduate.Dao.LearningRecordDao;
import com.zjut.graduate.Dao.MistakeAnalysisDao;
import com.zjut.graduate.Dao.QuestionBankDao;
import com.zjut.graduate.Dao.QuestionKnowledgePointRelDao;
import com.zjut.graduate.Po.KnowledgePoint;
import com.zjut.graduate.Po.LearningRecord;
import com.zjut.graduate.Po.MistakeAnalysis;
import com.zjut.graduate.Po.QuestionBank;
import com.zjut.graduate.Service.DeepSeekProxyService;
import com.zjut.graduate.Service.MistakeDeepAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MistakeDeepAnalysisServiceImpl implements MistakeDeepAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(MistakeDeepAnalysisServiceImpl.class);

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "概念混淆", "推理缺失", "审题偏差", "知识遗忘", "细节失误");

    private final DeepSeekProxyService deepSeekProxyService;
    private final LearningRecordDao learningRecordDao;
    private final QuestionBankDao questionBankDao;
    private final QuestionKnowledgePointRelDao questionKnowledgePointRelDao;
    private final KnowledgePointDao knowledgePointDao;
    private final MistakeAnalysisDao mistakeAnalysisDao;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public MistakeDeepAnalysisServiceImpl(DeepSeekProxyService deepSeekProxyService,
                                          LearningRecordDao learningRecordDao,
                                          QuestionBankDao questionBankDao,
                                          QuestionKnowledgePointRelDao questionKnowledgePointRelDao,
                                          KnowledgePointDao knowledgePointDao,
                                          MistakeAnalysisDao mistakeAnalysisDao) {
        this.deepSeekProxyService = deepSeekProxyService;
        this.learningRecordDao = learningRecordDao;
        this.questionBankDao = questionBankDao;
        this.questionKnowledgePointRelDao = questionKnowledgePointRelDao;
        this.knowledgePointDao = knowledgePointDao;
        this.mistakeAnalysisDao = mistakeAnalysisDao;
    }

    @Async
    @Override
    public void analyzeAsync(Long learningRecordId) {
        if (learningRecordId == null) {
            return;
        }
        try {
            analyzeAndSave(learningRecordId);
        } catch (Exception e) {
            log.warn("错题深度分析失败 recordId={}", learningRecordId, e);
        }
    }

    private void analyzeAndSave(Long recordId) throws Exception {
        LearningRecord lr = learningRecordDao.selectById(recordId);
        if (lr == null) {
            log.warn("错题分析跳过：learning_record 不存在 recordId={}", recordId);
            return;
        }
        if (lr.getIsCorrect() == null || lr.getIsCorrect() != 0) {
            return;
        }
        QuestionBank qb = questionBankDao.selectById(lr.getQuestionId());
        if (qb == null) {
            return;
        }

        Long kpId = questionKnowledgePointRelDao.selectFirstKpIdByQuestionId(qb.getId());
        String kpLabel = "";
        if (kpId != null) {
            KnowledgePoint kp = knowledgePointDao.selectById(kpId);
            if (kp != null && kp.getName() != null) {
                kpLabel = kp.getName();
            }
        }

        if (!deepSeekProxyService.isConfigured()) {
            persistAnalysis(recordId, kpId, "细节失误",
                    "系统未配置大模型 API，无法生成智能分析。请对照教材与讲义自行总结错因与要点。",
                    null, null);
            return;
        }

        String userPayload = buildUserPayload(qb, lr, kpLabel);
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> sys = new LinkedHashMap<>();
        sys.put("role", "system");
        sys.put("content", buildSystemPrompt());
        messages.add(sys);
        Map<String, String> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", userPayload);
        messages.add(user);

        String raw;
        try {
            raw = deepSeekProxyService.chatDirect(messages, 0.25);
        } catch (Exception e) {
            persistAnalysis(recordId, kpId, "细节失误",
                    "暂时无法连接大模型完成分析。请稍后刷新错题本，或自行结合知识点复盘。",
                    null, e.getMessage());
            return;
        }

        Parsed parsed = parseModelJson(raw);
        String errorType = normalizeErrorType(parsed.errorType);
        String suggestion = parsed.suggestion == null || parsed.suggestion.trim().isEmpty()
                ? "建议回顾本题涉及的核心概念，并独立完成 1～2 道同类题巩固。"
                : parsed.suggestion.trim();

        persistAnalysis(recordId, kpId, errorType, suggestion, raw, null);
    }

    private void persistAnalysis(Long recordId, Long kpId, String errorType, String suggestion,
                                 String rawLlm, String errNote) {
        String raw = rawLlm;
        if (errNote != null) {
            raw = errNote;
        }
        MistakeAnalysis existing = mistakeAnalysisDao.selectByRecordId(recordId);
        Date now = new Date();
        if (existing != null && existing.getId() != null) {
            existing.setKpId(kpId);
            existing.setErrorType(errorType);
            existing.setSuggestion(suggestion);
            existing.setRawLlmOutput(raw);
            mistakeAnalysisDao.update(existing);
        } else {
            MistakeAnalysis ma = new MistakeAnalysis();
            ma.setRecordId(recordId);
            ma.setKpId(kpId);
            ma.setErrorType(errorType);
            ma.setSuggestion(suggestion);
            ma.setRawLlmOutput(raw);
            ma.setCreatedAt(now);
            mistakeAnalysisDao.insert(ma);
        }
    }

    private static String buildSystemPrompt() {
        return "你是计算机专业课程的助教，负责错题归因与学习建议。\n"
                + "你必须只输出一个 JSON 对象，不要 Markdown、不要代码块、不要其它说明文字。\n"
                + "JSON 字段：\n"
                + "1) error_type：字符串，必须是以下之一：概念混淆、推理缺失、审题偏差、知识遗忘、细节失误。\n"
                + "2) suggestion：字符串，用中文写给学生的具体建议，2～5 句，紧扣题目与错因。\n"
                + "概念混淆=定义/性质理解错误；推理缺失=推导或步骤断裂；审题偏差=漏看或误解题意；"
                + "知识遗忘=结论记错；细节失误=思路对但边界/符号等出错。";
    }

    private String buildUserPayload(QuestionBank qb, LearningRecord lr, String knowledgePointLabel) {
        StringBuilder sb = new StringBuilder();
        sb.append("题干：").append(nullToEmpty(qb.getContent())).append("\n");
        sb.append("选项：\n").append(formatOptions(qb.getOptions())).append("\n");
        sb.append("正确答案：").append(nullToEmpty(qb.getCorrectAnswer())).append("\n");
        sb.append("学生作答：").append(nullToEmpty(lr.getUserAnswer())).append("\n");
        sb.append("关联知识点（若有）：").append(knowledgePointLabel == null || knowledgePointLabel.isEmpty() ? "无" : knowledgePointLabel);
        return sb.toString();
    }

    private String formatOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.trim().isEmpty()) {
            return "（无）";
        }
        try {
            JsonNode node = objectMapper.readTree(optionsJson);
            StringBuilder sb = new StringBuilder();
            for (String k : new String[]{"A", "B", "C", "D"}) {
                if (node.has(k)) {
                    sb.append(k).append(". ").append(node.get(k).asText("")).append("\n");
                }
            }
            return sb.length() == 0 ? optionsJson : sb.toString();
        } catch (Exception e) {
            return optionsJson;
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private Parsed parseModelJson(String text) throws Exception {
        Parsed p = new Parsed();
        if (text == null) {
            return p;
        }
        String t = text.trim();
        int i = t.indexOf('{');
        int j = t.lastIndexOf('}');
        if (i < 0 || j <= i) {
            return p;
        }
        JsonNode root = objectMapper.readTree(t.substring(i, j + 1));
        p.errorType = root.path("error_type").asText("");
        p.suggestion = root.path("suggestion").asText("");
        return p;
    }

    private String normalizeErrorType(String raw) {
        if (raw == null) {
            return "审题偏差";
        }
        String s = raw.trim();
        for (String a : ALLOWED_TYPES) {
            if (a.equals(s)) {
                return a;
            }
        }
        String lower = s.toLowerCase(Locale.ROOT);
        Map<String, String> legacy = new HashMap<>();
        legacy.put("concept", "概念混淆");
        legacy.put("reasoning", "推理缺失");
        legacy.put("careless", "细节失误");
        legacy.put("memory", "知识遗忘");
        if (legacy.containsKey(lower)) {
            return legacy.get(lower);
        }
        if (s.contains("概念")) {
            return "概念混淆";
        }
        if (s.contains("推理") || s.contains("推导")) {
            return "推理缺失";
        }
        if (s.contains("审题") || s.contains("读题")) {
            return "审题偏差";
        }
        if (s.contains("遗忘") || s.contains("记忆")) {
            return "知识遗忘";
        }
        if (s.contains("细节") || s.contains("粗心")) {
            return "细节失误";
        }
        return "审题偏差";
    }

    private static class Parsed {
        String errorType;
        String suggestion;
    }
}
