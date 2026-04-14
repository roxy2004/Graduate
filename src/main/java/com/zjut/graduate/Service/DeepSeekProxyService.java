package com.zjut.graduate.Service;

import java.util.List;
import java.util.Map;

public interface DeepSeekProxyService {
    String chat(List<Map<String, Object>> clientMessages) throws Exception;
}
