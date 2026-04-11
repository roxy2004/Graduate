package com.zjut.graduate.Service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjut.graduate.Dao.LearningRecordDao;
import com.zjut.graduate.Dao.MistakeAnalysisDao;
import com.zjut.graduate.Dao.QuestionBankDao;
import com.zjut.graduate.Dao.QuestionKnowledgePointRelDao;
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
import java.util.List;
import java.util.Map;

@Service
public class QuestionBankServiceImpl implements QuestionBankService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private QuestionBankDao questionBankDao;

    @Autowired
    private QuestionKnowledgePointRelDao questionKnowledgePointRelDao;

    @Autowired
    private MistakeAnalysisDao mistakeAnalysisDao;

    @Autowired
    private LearningRecordDao learningRecordDao;

    @Override
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
                if (isBlank(row.get("content"))) {
                    continue;
                }
                QuestionBank question = new QuestionBank();
                question.setContent(row.get("content").trim());
                question.setImageUrl(emptyToNull(getValue(row, "image_url", "imageUrl")));
                String options = resolveOptions(row);
                if (isBlank(options)) {
                    continue;
                }
                question.setOptions(options);
                question.setCorrectAnswer(getValue(row, "correct_answer", "correctAnswer").trim().toUpperCase());
                question.setDifficulty(parseDifficulty(getValue(row, "difficulty")));
                question.setKnowledgePointIds(getValue(row, "knowledge_point_ids", "knowledgePointIds").trim());
                question.setQuestionType("choice");
                String sourceTag = emptyToNull(getValue(row, "source_tag", "sourceTag"));
                question.setSourceTag(sourceTag != null ? sourceTag : "教师导入");
                question.setStatus(1);
                question.setCreatedBy(createdByUserId);
                question.setCreatedAt(new Date());
                questionBankDao.insert(question);
                saveKnowledgePointRelations(question.getId(), question.getKnowledgePointIds());
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

    private void saveKnowledgePointRelations(Long questionId, String knowledgePointIdsCsv) {
        if (questionId == null || isBlank(knowledgePointIdsCsv)) {
            return;
        }
        for (String part : knowledgePointIdsCsv.split(",")) {
            if (isBlank(part)) {
                continue;
            }
            try {
                long kpId = Long.parseLong(part.trim());
                questionKnowledgePointRelDao.insert(questionId, kpId);
            } catch (NumberFormatException ignored) {
                // 跳过非法知识点 ID
            }
        }
    }

    private Map<String, String> toRowMap(List<String> headers, List<String> values) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String key = headers.get(i) == null ? "" : headers.get(i).trim();
            String value = i < values.size() ? values.get(i) : "";
            map.put(key, value == null ? "" : value);
        }
        return map;
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
            if (row.containsKey(key)) {
                return row.get(key);
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
