package com.zjut.graduate.Po;

import java.util.Date;

public class MistakeAnalysis {
    private Long id;
    private Long recordId;
    private String knowledgePoint;
    private String errorType;     // 概念混淆/推理缺失/审题偏差
    private String suggestion;
    private String rawLlmOutput;
    private Date createdAt;
}
