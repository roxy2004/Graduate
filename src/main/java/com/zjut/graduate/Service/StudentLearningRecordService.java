package com.zjut.graduate.Service;

import java.util.List;
import java.util.Map;

public interface StudentLearningRecordService {
    List<Map<String, Object>> listRecords(Long userId);

    boolean deleteRecord(Long userId, Long recordId);

    int clearAll(Long userId);
}

