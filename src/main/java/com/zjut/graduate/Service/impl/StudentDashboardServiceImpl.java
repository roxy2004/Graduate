package com.zjut.graduate.Service.impl;

import com.zjut.graduate.Dao.LearningRecordDao;
import com.zjut.graduate.Service.StudentDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StudentDashboardServiceImpl implements StudentDashboardService {

    @Autowired
    private LearningRecordDao learningRecordDao;

    @Override
    public Map<String, Object> getDashboardStats(Long userId) {
        int solvedCount = learningRecordDao.countByUserId(userId);
        int correctCount = learningRecordDao.countCorrectByUserId(userId);
        int learningDays = learningRecordDao.countLearningDaysByUserId(userId);
        double accuracy = solvedCount == 0 ? 0D : (correctCount * 100D / solvedCount);

        Map<String, Object> data = new HashMap<>();
        data.put("solvedCount", solvedCount);
        data.put("correctCount", correctCount);
        data.put("accuracy", Math.round(accuracy * 10D) / 10D);
        data.put("learningDays", learningDays);
        return data;
    }
}

