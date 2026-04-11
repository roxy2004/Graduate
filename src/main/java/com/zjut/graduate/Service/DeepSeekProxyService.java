package com.zjut.graduate.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DeepSeekProxyService {

    private static final int MAX_MESSAGES = 32;
    private static final int MAX_CONTENT_LEN = 6000;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${graduate.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${graduate.deepseek.api-key:}")
    private String apiKey;

    @Value("${graduate.deepseek.model:deepseek-chat}")
    private String model;

    @Autowired
    public DeepSeekProxyService(@Qualifier("deepSeekRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String chat(List<Map<String, Object>> clientMessages) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("未配置 DeepSeek API Key，请在环境变量 SPRING_AI_OPENAI_API_KEY 或 graduate.deepseek.api-key 中设置");
        }
        List<Map<String, String>> messages = normalizeMessages(clientMessages);
        String url = buildChatUrl();
        Map<String, Object> body = new HashMap<>();
        body.put("model", model.trim());
        body.put("messages", messages);
        body.put("temperature", 0.6);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("DeepSeek 返回异常状态: " + response.getStatusCode());
            }
            return parseAssistantContent(response.getBody());
        } catch (RestClientException e) {
            throw new IllegalStateException("调用 DeepSeek 失败: " + e.getMessage(), e);
        }
    }

    private String buildChatUrl() {
        String base = baseUrl == null ? "https://api.deepseek.com" : baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!base.endsWith("/v1")) {
            base = base + "/v1";
        }
        return base + "/chat/completions";
    }

    private List<Map<String, String>> normalizeMessages(List<Map<String, Object>> clientMessages) {
        List<Map<String, String>> out = new ArrayList<>();
        Map<String, String> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", "你是面向计算机专业学生的学习助手，回答简洁准确，可结合数据结构、操作系统、计算机网络等课程知识。不要编造不存在的政策或链接。");
        out.add(system);

        if (clientMessages == null) {
            return out;
        }
        int start = Math.max(0, clientMessages.size() - MAX_MESSAGES);
        for (int i = start; i < clientMessages.size(); i++) {
            Map<String, Object> row = clientMessages.get(i);
            if (row == null) {
                continue;
            }
            Object roleObj = row.get("role");
            Object contentObj = row.get("content");
            String role = roleObj == null ? "" : String.valueOf(roleObj).trim().toLowerCase(Locale.ROOT);
            String content = contentObj == null ? "" : String.valueOf(contentObj).trim();
            if (content.isEmpty()) {
                continue;
            }
            if (content.length() > MAX_CONTENT_LEN) {
                content = content.substring(0, MAX_CONTENT_LEN);
            }
            if (!"user".equals(role) && !"assistant".equals(role)) {
                continue;
            }
            Map<String, String> m = new LinkedHashMap<>();
            m.put("role", role);
            m.put("content", content);
            out.add(m);
        }
        if (out.size() == 1) {
            throw new IllegalArgumentException("请至少发送一条用户消息");
        }
        return out;
    }

    private String parseAssistantContent(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        if (root.has("error")) {
            String msg = root.get("error").path("message").asText("未知错误");
            throw new IllegalStateException("DeepSeek API: " + msg);
        }
        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.size() == 0) {
            throw new IllegalStateException("DeepSeek 返回无 choices 内容");
        }
        return choices.get(0).path("message").path("content").asText("").trim();
    }
}
