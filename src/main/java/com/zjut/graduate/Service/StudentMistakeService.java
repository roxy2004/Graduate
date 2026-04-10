package com.zjut.graduate.Service;

import java.util.List;
import java.util.Map;

public interface StudentMistakeService {
    List<Map<String, Object>> listMistakes(Long userId);

    Map<String, Object> redo(Long userId, Long recordId, String answer);
}

