package com.zjut.graduate.Dao;

import com.zjut.graduate.Po.DailyRecommendationSnapshot;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DailyRecommendationSnapshotDao {

    @Select("SELECT * FROM daily_recommendation_snapshot " +
            "WHERE user_id = #{userId} AND snapshot_date = CURDATE() AND mode = #{mode} " +
            "ORDER BY id DESC LIMIT 1")
    DailyRecommendationSnapshot selectTodayByUserAndMode(@Param("userId") Long userId, @Param("mode") String mode);

    @Insert("INSERT INTO daily_recommendation_snapshot (user_id, snapshot_date, mode, payload_json, created_at, updated_at) " +
            "VALUES (#{userId}, CURDATE(), #{mode}, #{payloadJson}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DailyRecommendationSnapshot snapshot);

    @Update("UPDATE daily_recommendation_snapshot SET payload_json = #{payloadJson}, updated_at = NOW() WHERE id = #{id}")
    int updatePayloadById(@Param("id") Long id, @Param("payloadJson") String payloadJson);
}
