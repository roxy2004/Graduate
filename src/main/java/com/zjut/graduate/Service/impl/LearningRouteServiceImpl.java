package com.zjut.graduate.Service.impl;

import com.zjut.graduate.Dao.LearningRouteDao;
import com.zjut.graduate.Po.LearningRoute;
import com.zjut.graduate.Po.LearningRouteItem;
import com.zjut.graduate.Service.LearningRouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LearningRouteServiceImpl implements LearningRouteService {

    @Autowired
    private LearningRouteDao learningRouteDao;

    @Override
    public Map<String, Object> getLatestRoute(Long userId) {
        Map<String, Object> data = new HashMap<>();
        LearningRoute route = learningRouteDao.selectLatestByUserId(userId);
        if (route == null) {
            data.put("route", null);
            data.put("items", new ArrayList<LearningRouteItem>());
            return data;
        }
        List<LearningRouteItem> items = learningRouteDao.selectItemsByRouteId(route.getId());
        data.put("route", route);
        data.put("items", items);
        return data;
    }
}

