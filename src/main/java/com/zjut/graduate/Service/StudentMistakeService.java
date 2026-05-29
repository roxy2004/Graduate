package com.zjut.graduate.Service;

import java.util.List;
import java.util.Map;

/** 学生错题本 */
public interface StudentMistakeService {

    /** 查询错题列表 */
    List<Map<String, Object>> listMistakes(Long userId);

    /** 错题重做并返回判分结果 */
    Map<String, Object> redo(Long userId, Long recordId, String answer);
}
