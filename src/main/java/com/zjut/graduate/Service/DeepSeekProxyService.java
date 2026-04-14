package com.zjut.graduate.Service;

import java.util.List;
import java.util.Map;

public interface DeepSeekProxyService {

    boolean isConfigured();

    String chat(List<Map<String, Object>> clientMessages) throws Exception;

    String chatDirect(List<Map<String, String>> messages, double temperature) throws Exception;
}
