package com.zjut.graduate.Service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjut.graduate.Dao.CourseSectionDao;
import com.zjut.graduate.Dao.KnowledgePointDao;
import com.zjut.graduate.Dao.LearningRecordDao;
import com.zjut.graduate.Dao.MistakeAnalysisDao;
import com.zjut.graduate.Dao.QuestionBankDao;
import com.zjut.graduate.Dao.QuestionKnowledgePointRelDao;
import com.zjut.graduate.Po.CourseSection;
import com.zjut.graduate.Po.KnowledgePoint;
import com.zjut.graduate.Po.QuestionBank;
import com.zjut.graduate.Service.QuestionBankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class QuestionBankServiceImpl implements QuestionBankService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 与课程小节 id 对齐的练习知识点 id：88000000 + course_section.id */
    private static final long SYNTHETIC_KP_BASE = 88_000_000L;

    @Autowired
    private QuestionBankDao questionBankDao;

    @Autowired
    private QuestionKnowledgePointRelDao questionKnowledgePointRelDao;

    @Autowired
    private KnowledgePointDao knowledgePointDao;

    @Autowired
    private CourseSectionDao courseSectionDao;

    @Autowired
    private MistakeAnalysisDao mistakeAnalysisDao;

    @Autowired
    private LearningRecordDao learningRecordDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importQuestionsFromCsv(MultipartFile file, Long createdByUserId) {
        if (file == null || file.isEmpty()) {
            return 0;
        }
        int importedCount = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            List<String> headers = null;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                if (headers == null) {
                    headers = parseCsvLine(line);
                    continue;
                }
                List<String> cols = parseCsvLine(line);
                Map<String, String> row = toRowMap(headers, cols);
                if (isBlank(getValue(row, "content"))) {
                    continue;
                }
                String options = resolveOptions(row);
                if (isBlank(options)) {
                    continue;
                }
                String correctRaw = getValue(row, "correct_answer", "correctAnswer").trim();
                if (isBlank(correctRaw)) {
                    continue;
                }
                QuestionBank question = new QuestionBank();
                question.setContent(getValue(row, "content").trim());
                question.setImageUrl(emptyToNull(getValue(row, "image_url", "imageUrl")));
                question.setOptions(options);
                question.setCorrectAnswer(correctRaw.toUpperCase());
                question.setDifficulty(parseDifficulty(getValue(row, "difficulty")));
                question.setKnowledgePointIds(getValue(row, "knowledge_point_ids", "knowledgePointIds", "kp_ids").trim());
                question.setQuestionType("choice");
                String sourceTag = emptyToNull(getValue(row, "source_tag", "sourceTag"));
                question.setSourceTag(sourceTag != null ? sourceTag : "教师导入");
                question.setStatus(1);
                question.setCreatedBy(createdByUserId);
                question.setCreatedAt(new Date());
                questionBankDao.insert(question);
                saveKnowledgePointRelations(question.getId(), row);
                importedCount++;
            }
        } catch (IOException e) {
            throw new RuntimeException("读取CSV失败", e);
        }
        return importedCount;
    }

    @Override
    public List<QuestionBank> listAllQuestions() {
        return questionBankDao.selectAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteQuestion(Long questionId) {
        if (questionId == null) {
            return false;
        }
        mistakeAnalysisDao.deleteByQuestionId(questionId);
        learningRecordDao.deleteByQuestionId(questionId);
        questionKnowledgePointRelDao.deleteByQuestionId(questionId);
        return questionBankDao.deleteById(questionId) > 0;
    }

    /**
     * 学生刷题依赖 question_knowledge_point_rel + question_bank.status=1。
     * 支持：knowledge_point_ids；knowledge_point_name(s)（按名称匹配或自动新建）；
     * course_section_id / section_id（自动挂到 88000000+小节id 对应知识点，与小节标题同步）。
     */
    private void saveKnowledgePointRelations(Long questionId, Map<String, String> row) {
        if (questionId == null) {
            return;
        }
        Set<Long> kpIds = new LinkedHashSet<>();

        String sectionRaw = getValue(row, "course_section_id", "section_id", "小节id").trim();
        if (!isBlank(sectionRaw)) {
            try {
                long sid = Long.parseLong(sectionRaw);
                CourseSection cs = courseSectionDao.selectActiveById(sid);
                if (cs != null) {
                    long syntheticId = SYNTHETIC_KP_BASE + sid;
                    String title = cs.getTitle() == null ? "" : cs.getTitle().trim();
                    String kpName = title.isEmpty() ? ("小节" + sid + "【练习】") : (title + "【练习】");
                    knowledgePointDao.upsertById(syntheticId, kpName);
                    kpIds.add(syntheticId);
                }
            } catch (NumberFormatException ignored) {
                // 忽略非法小节 id
            }
        }

        String nameHint = getValue(row, "knowledge_point_name", "knowledgePointName").trim();
        String idsCsv = getValue(row, "knowledge_point_ids", "knowledgePointIds", "kp_ids").trim();
        if (!isBlank(idsCsv)) {
            for (String part : idsCsv.split("[,，]")) {
                if (isBlank(part)) {
                    continue;
                }
                try {
                    long kpId = Long.parseLong(part.trim());
                    ensureKnowledgePointForNumericId(kpId, nameHint);
                    kpIds.add(kpId);
                } catch (NumberFormatException ignored) {
                    // 跳过非数字
                }
            }
        }

        String namesBlob = getValue(row, "knowledge_point_names", "knowledgePointNames").trim();
        if (isBlank(namesBlob)) {
            namesBlob = nameHint;
        }
        if (!isBlank(namesBlob)) {
            for (String part : namesBlob.split("[,，;；]")) {
                if (isBlank(part)) {
                    continue;
                }
                Long id = resolveOrCreateKnowledgePointByName(part.trim());
                if (id != null) {
                    kpIds.add(id);
                }
            }
        }

        for (Long kpId : kpIds) {
            if (knowledgePointDao.selectById(kpId) == null) {
                continue;
            }
            if (questionKnowledgePointRelDao.countByQuestionAndKp(questionId, kpId) > 0) {
                continue;
            }
            questionKnowledgePointRelDao.insert(questionId, kpId);
        }
    }

    private void ensureKnowledgePointForNumericId(long kpId, String nameHint) {
        if (knowledgePointDao.selectById(kpId) != null) {
            return;
        }
        String name = !isBlank(nameHint) ? nameHint.trim() : ("教师导入-知识点" + kpId);
        knowledgePointDao.upsertById(kpId, name);
    }

    private Long resolveOrCreateKnowledgePointByName(String name) {
        if (isBlank(name)) {
            return null;
        }
        Long id = knowledgePointDao.selectIdByExactName(name);
        if (id != null) {
            return id;
        }
        if (!name.contains("【练习】")) {
            id = knowledgePointDao.selectIdByExactName(name + "【练习】");
            if (id != null) {
                return id;
            }
        } else {
            String stripped = name.replace("【练习】", "").trim();
            if (!stripped.isEmpty()) {
                id = knowledgePointDao.selectIdByExactName(stripped);
                if (id != null) {
                    return id;
                }
            }
        }
        KnowledgePoint kp = new KnowledgePoint();
        kp.setName(name.trim());
        knowledgePointDao.insert(kp);
        return kp.getId();
    }

    private Map<String, String> toRowMap(List<String> headers, List<String> values) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String key = normalizeCsvHeader(headers.get(i));
            String value = i < values.size() ? values.get(i) : "";
            if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
                value = value.substring(1);
            }
            value = value == null ? "" : value;
            if (!key.isEmpty()) {
                map.put(key, value);
            }
        }
        return map;
    }

    private static String normalizeCsvHeader(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') {
            s = s.substring(1).trim();
        }
        return s.toLowerCase(Locale.ROOT);
    }

    private String resolveOptions(Map<String, String> row) {
        String optionsJson = getValue(row, "options");
        if (!isBlank(optionsJson)) {
            return normalizeOptionsJson(optionsJson);
        }
        return buildOptionsJson(
                getValue(row, "A"),
                getValue(row, "B"),
                getValue(row, "C"),
                getValue(row, "D")
        );
    }

    private String normalizeOptionsJson(String raw) {
        String value = raw.trim();
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start >= 0 && end > start) {
            value = value.substring(start, end + 1);
        }
        value = value.replace("“", "\"").replace("”", "\"");
        String canonical = tryCanonicalizeJson(value);
        if (canonical != null) {
            return canonical;
        }
        // 容错：把单引号 JSON 转为双引号再试一次
        canonical = tryCanonicalizeJson(value.replace('\'', '"'));
        return canonical == null ? null : canonical;
    }

    private String getValue(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String k = key.toLowerCase(Locale.ROOT);
            if (row.containsKey(k)) {
                String v = row.get(k);
                return v == null ? "" : v;
            }
        }
        return "";
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values;
    }

    private String buildOptionsJson(String a, String b, String c, String d) {
        return "{\"A\":\"" + escapeJson(a.trim()) + "\",\"B\":\"" + escapeJson(b.trim()) +
                "\",\"C\":\"" + escapeJson(c.trim()) + "\",\"D\":\"" + escapeJson(d.trim()) + "\"}";
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Double parseDifficulty(String raw) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (Exception e) {
            return 0.5D;
        }
    }

    private String emptyToNull(String value) {
        String v = value == null ? "" : value.trim();
        return v.isEmpty() ? null : v;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String tryCanonicalizeJson(String raw) {
        try {
            return OBJECT_MAPPER.writeValueAsString(OBJECT_MAPPER.readTree(raw));
        } catch (Exception e) {
            return null;
        }
    }
}
