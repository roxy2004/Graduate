package com.zjut.graduate.Dao;

import com.zjut.graduate.Po.LearningRoute;
import com.zjut.graduate.Po.LearningRouteItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LearningRouteDao {

    @Select("SELECT * FROM learning_route WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT 1")
    LearningRoute selectLatestByUserId(Long userId);

    @Select("SELECT * FROM learning_route_item WHERE route_id = #{routeId} ORDER BY sort_no ASC")
    List<LearningRouteItem> selectItemsByRouteId(Long routeId);
}

