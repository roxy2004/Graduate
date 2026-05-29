package com.zjut.graduate.Service;

import java.util.List;
import java.util.Map;

/** 学生做题记录查询与清理 */
public interface StudentLearningRecordService {

    /** 查询全部做题记录 */
    List<Map<String, Object>> listRecords(Long userId);

    /** 删除单条记录 */
    boolean deleteRecord(Long userId, Long recordId);

    /** 清空全部记录，返回删除条数 */
    int clearAll(Long userId);
}
