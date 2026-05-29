package com.zjut.graduate.Service;

import java.util.Map;

/** 学生首页聚合数据 */
public interface StudentHomeService {

    /** 首页概览：统计 + 轻量推荐 */
    Map<String, Object> getOverview(Long userId);
}
