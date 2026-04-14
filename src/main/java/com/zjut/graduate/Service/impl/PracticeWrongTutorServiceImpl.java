package com.zjut.graduate.Service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjut.graduate.Dao.CourseSectionDao;
import com.zjut.graduate.Dao.KnowledgePointDao;
import com.zjut.graduate.Dao.QuestionBankDao;
import com.zjut.graduate.Dao.QuestionKnowledgePointRelDao;
import com.zjut.graduate.Po.CourseSection;
import com.zjut.graduate.Po.KnowledgePoint;
import com.zjut.graduate.Po.QuestionBank;
import com.zjut.graduate.Service.DeepSeekProxyService;
import com.zjut.graduate.Service.LearningContentService;
import com.zjut.graduate.Service.PracticeWrongTutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PracticeWrongTutorServiceImpl implements PracticeWrongTutorService {

    private static final int MAX_NOTE_LEN = 8000;

    private final DeepSeekProxyService deepSeekProxyService;
    private final QuestionBankDao questionBankDao;
    private final QuestionKnowledgePointRelDao questionKnowledgePointRelDao;
    private final KnowledgePointDao knowledgePointDao;
    private final CourseSectionDao courseSectionDao;
    private final LearningContentService learningContentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public PracticeWrongTutorServiceImpl(DeepSeekProxyService deepSeekProxyService,
                                         QuestionBankDao questionBankDao,
                                         QuestionKnowledgePointRelDao questionKnowledgePointRelDao,
                                         KnowledgePointDao knowledgePointDao,
                                         CourseSectionDao courseSectionDao,
                                         LearningContentService learningContentService) {
        this.deepSeekProxyService = deepSeekProxyService;
        this.questionBankDao = questionBankDao;
        this.questionKnowledgePointRelDao = questionKnowledgePointRelDao;
        this.knowledgePointDao = knowledgePointDao;
        this.courseSectionDao = courseSectionDao;
        this.learningContentService = learningContentService;
    }

    @Override
    public Map<String, Object> explainWrongAnswer(Long userId, Long kpId, Long questionId, String userAnswer) {
        Map<String, Object> out = new HashMap<>();
        if (userId == null || kpId == null || questionId == null) {
            out.put("status", "error");
            out.put("message", "参数不完整");
            return out;
        }
        if (questionKnowledgePointRelDao.countByQuestionAndKp(questionId, kpId) <= 0) {
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
        String ua = userAnswer == null ? "" : userAnswer.trim().toUpperCase(Locale.ROOT);
        if (ua.isEmpty()) {
            out.put("status", "error");
            out.put("message", "请提供学生作答");
            return out;
        }
        String correct = qb.getCorrectAnswer() == null ? "" : qb.getCorrectAnswer().trim();
        if (correct.equalsIgnoreCase(ua)) {
            out.put("status", "error");
            out.put("message", "该作答为正确，无需错题解析");
            return out;
        }

        KnowledgePoint kp = knowledgePointDao.selectById(kpId);
        String kpName = kp == null ? "该知识点" : (kp.getName() == null ? "该知识点" : kp.getName());
        Long sectionId = resolvePracticeNoteSectionId(kp, kpId);
        String sectionTitle = null;
        if (sectionId != null) {
            CourseSection sec = courseSectionDao.selectActiveById(sectionId);
            if (sec != null) {
                sectionTitle = sec.getTitle();
            }
        }

        String explanation;
        String knowledgeNotes;
        if (!deepSeekProxyService.isConfigured()) {
            explanation = "系统未配置大模型 API，无法生成自动解析。请结合教材与课堂讲解自行梳理本题。";
            knowledgeNotes = "建议复习「" + kpName + "」的定义、典型例题与易错点；可在专项学习对应小节中补充自己的理解。";
        } else {
            try {
                Parsed p = callLlm(qb, ua, correct, kpName);
                explanation = p.explanation;
                knowledgeNotes = p.knowledgeNotes;
            } catch (Exception e) {
                explanation = "暂时无法生成解析（" + e.getMessage() + "）。请稍后再试或询问教师。";
                knowledgeNotes = "请对照「" + kpName + "」相关章节完成订正。";
            }
        }

        out.put("status", "success");
        out.put("explanation", explanation);
        out.put("knowledgeNotes", knowledgeNotes);
        out.put("kpName", kpName);
        out.put("targetSectionId", sectionId);
        out.put("targetSectionTitle", sectionTitle);
        out.put("canFavorite", sectionId != null);
        return out;
    }

    @Override
    @Transactional
    public Map<String, Object> favoriteTutorNote(Long userId, Long kpId, Long questionId, String noteBody) {
        Map<String, Object> out = new HashMap<>();
        if (userId == null || kpId == null || questionId == null) {
            out.put("status", "error");
            out.put("message", "参数不完整");
            return out;
        }
        if (noteBody == null || noteBody.trim().isEmpty()) {
            out.put("status", "error");
            out.put("message", "笔记内容不能为空");
            return out;
        }
        if (questionKnowledgePointRelDao.countByQuestionAndKp(questionId, kpId) <= 0) {
            out.put("status", "error");
            out.put("message", "题目不属于该知识点");
            return out;
        }
        KnowledgePoint kp = knowledgePointDao.selectById(kpId);
        Long sectionId = resolvePracticeNoteSectionId(kp, kpId);
        if (sectionId == null) {
            out.put("status", "error");
            out.put("message", "未找到该知识点对应的小节：请让小节标题与知识点名称一致，或执行迁移 sql/migrate_knowledge_point_anchor_section.sql 后配置 anchor_section_id");
            return out;
        }
        String body = noteBody.trim();
        if (body.length() > MAX_NOTE_LEN) {
            body = body.substring(0, MAX_NOTE_LEN);
        }
        learningContentService.addLearningNote(userId, sectionId, body, 0);
        out.put("status", "success");
        out.put("message", "已收藏到专项学习笔记");
        out.put("sectionId", sectionId);
        return out;
    }

    /**
     * 若表已含 anchor_section_id 且已赋值，则优先使用（由 {@link KnowledgePointDao#selectById} 的 SELECT * 映射）；
     * 否则按小节标题与知识点名的模糊匹配（见 {@link KnowledgePointDao#selectSectionIdByKnowledgePointTitleMatch}）。
     */
    private Long resolvePracticeNoteSectionId(KnowledgePoint kp, Long kpId) {
        if (kp != null && kp.getAnchorSectionId() != null) {
            return kp.getAnchorSectionId();
        }
        return knowledgePointDao.selectSectionIdByKnowledgePointTitleMatch(kpId);
    }

    private Parsed callLlm(QuestionBank qb, String userAnswer, String correctAnswer, String kpName) throws Exception {
        String userBlock = buildQuestionBlock(qb, userAnswer, correctAnswer, kpName);
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> sys = new LinkedHashMap<>();
        sys.put("role", "system");
        sys.put("content", "你是计算机专业助教。学生练习题做错了，请给出解析与知识点笔记。\n"
                + "只输出一个 JSON 对象，不要 Markdown、代码块或其它文字。\n"
                + "字段：explanation（中文，2～5 句，说明错因与正确思路）、"
                + "knowledge_notes（中文，分条或分段，给出可背诵/可复用的知识点小结，紧扣「" + kpName + "」）。");
        messages.add(sys);
        Map<String, String> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", userBlock);
        messages.add(user);
        String raw = deepSeekProxyService.chatDirect(messages, 0.3);
        return parseTutorJson(raw);
    }

    private String buildQuestionBlock(QuestionBank qb, String userAnswer, String correctAnswer, String kpName) {
        StringBuilder sb = new StringBuilder();
        sb.append("知识点：").append(kpName).append("\n题干：").append(nullToEmpty(qb.getContent())).append("\n选项：\n");
        sb.append(formatOptionsJson(qb.getOptions()));
        sb.append("正确答案：").append(correctAnswer).append("\n学生错选：").append(userAnswer);
        return sb.toString();
    }

    private String formatOptionsJson(String optionsJson) {
        if (optionsJson == null || optionsJson.trim().isEmpty()) {
            return "（无）\n";
        }
        try {
            JsonNode node = objectMapper.readTree(optionsJson);
            StringBuilder sb = new StringBuilder();
            for (String k : new String[]{"A", "B", "C", "D"}) {
                if (node.has(k)) {
                    sb.append(k).append(". ").append(node.get(k).asText("")).append("\n");
                }
            }
            return sb.length() == 0 ? optionsJson + "\n" : sb.toString();
        } catch (Exception e) {
            return optionsJson + "\n";
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private Parsed parseTutorJson(String text) throws Exception {
        Parsed p = new Parsed();
        if (text == null) {
            p.explanation = "";
            p.knowledgeNotes = "";
            return p;
        }
        String t = text.trim();
        int i = t.indexOf('{');
        int j = t.lastIndexOf('}');
        if (i < 0 || j <= i) {
            p.explanation = t;
            p.knowledgeNotes = "";
            return p;
        }
        JsonNode root = objectMapper.readTree(t.substring(i, j + 1));
        p.explanation = root.path("explanation").asText("").trim();
        p.knowledgeNotes = root.path("knowledge_notes").asText("").trim();
        return p;
    }

    private static class Parsed {
        String explanation;
        String knowledgeNotes;
    }
}
