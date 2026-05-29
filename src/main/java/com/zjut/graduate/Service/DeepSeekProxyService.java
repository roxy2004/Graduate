package com.zjut.graduate.Service;

import java.util.List;
import java.util.Map;

/** DeepSeek API 代理 */
public interface DeepSeekProxyService {

    /** 是否已配置 API Key */
    boolean isConfigured();

    /** 学生端多轮对话（OpenAI 兼容消息格式） */
    String chat(List<Map<String, Object>> clientMessages) throws Exception;

    /** 底层直连对话（指定温度） */
    String chatDirect(List<Map<String, String>> messages, double temperature) throws Exception;
}
