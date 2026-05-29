package com.zjut.graduate.Service;

import java.util.Map;

/** 个人中心学习趋势统计 */
public interface StudentLearningTrendService {

    /** 学习趋势：按日/累计正确率 + 高频知识点曲线 */
    Map<String, Object> buildTrendPayload(Long userId);
}
