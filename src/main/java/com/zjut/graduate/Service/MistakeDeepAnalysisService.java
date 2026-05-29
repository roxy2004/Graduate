package com.zjut.graduate.Service;

/** 错题深度分析（异步） */
public interface MistakeDeepAnalysisService {

    /** 异步触发错题深度分析 */
    void analyzeAsync(Long learningRecordId);
}
