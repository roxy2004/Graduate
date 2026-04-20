package com.zjut.graduate.Dao;

import com.zjut.graduate.Po.LearningRoute;
import com.zjut.graduate.Po.LearningRouteItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface LearningRouteDao {

    @Select("SELECT * FROM learning_route WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT 1")
    LearningRoute selectLatestByUserId(Long userId);

    @Select("SELECT * FROM learning_route_item WHERE route_id = #{routeId} ORDER BY sort_no ASC")
    List<LearningRouteItem> selectItemsByRouteId(Long routeId);

    @Insert("INSERT INTO learning_route (user_id, title, route_type, status, generated_by, summary, created_at) "
            + "VALUES (#{userId}, #{title}, #{routeType}, #{status}, #{generatedBy}, #{summary}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertRoute(LearningRoute route);

    @Update("UPDATE learning_route SET title = #{title}, summary = #{summary}, route_type = #{routeType}, "
            + "status = #{status}, generated_by = #{generatedBy} WHERE id = #{id}")
    void updateRouteMeta(LearningRoute route);

    @Delete("DELETE FROM learning_route_item WHERE route_id = #{routeId}")
    void deleteItemsByRouteId(Long routeId);

    @Insert("INSERT INTO learning_route_item (route_id, item_type, item_id, reason, priority, estimated_minutes, sort_no, completed) "
            + "VALUES (#{routeId}, #{itemType}, #{itemId}, #{reason}, #{priority}, #{estimatedMinutes}, #{sortNo}, #{completed})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertItem(LearningRouteItem item);
}
