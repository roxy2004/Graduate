package com.zjut.graduate.Service.impl;

import com.zjut.graduate.Dao.LearningRecordDao;
import com.zjut.graduate.Service.StudentLearningRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class StudentLearningRecordServiceImpl implements StudentLearningRecordService {

    @Autowired
    private LearningRecordDao learningRecordDao;

    @Override
    public List<Map<String, Object>> listRecords(Long userId) {
        return learningRecordDao.selectWithQuestionContentByUserId(userId);
    }

    @Override
    public boolean deleteRecord(Long userId, Long recordId) {
        return learningRecordDao.deleteByIdAndUserId(recordId, userId) > 0;
    }

    @Override
    public int clearAll(Long userId) {
        return learningRecordDao.deleteAllByUserId(userId);
    }
}

