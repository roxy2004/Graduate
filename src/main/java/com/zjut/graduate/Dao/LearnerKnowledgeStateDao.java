package com.zjut.graduate.Dao;

import com.zjut.graduate.Po.LearnerKnowledgeState;
import org.apache.ibatis.annotations.*;

import java.util.List;
@Mapper
public interface LearnerKnowledgeStateDao {
    @Select("SELECT * FROM learner_knowledge_state WHERE id = #{id}")
    LearnerKnowledgeState selectById(Long id);

    @Select("SELECT * FROM learner_knowledge_state WHERE user_id = #{userId} AND kp_id = #{kpId}")
    LearnerKnowledgeState selectByUserAndKp(@Param("userId") Long userId, @Param("kpId") Long kpId);

    @Select("SELECT * FROM learner_knowledge_state WHERE user_id = #{userId}")
    List<LearnerKnowledgeState> selectByUserId(Long userId);

    /**
     * 插入新的掌握度记录
     */
    @Insert("INSERT INTO learner_knowledge_state (user_id, kp_id, mastery_level, last_practiced_at, updated_at) " +
            "VALUES (#{userId}, #{kpId}, #{masteryLevel}, #{lastPracticedAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LearnerKnowledgeState state);

    /**
     * 更新掌握度和最后练习时间
     */
    @Update("UPDATE learner_knowledge_state SET mastery_level = #{masteryLevel}, last_practiced_at = #{lastPracticedAt}, updated_at = #{updatedAt} " +
            "WHERE user_id = #{userId} AND kp_id = #{kpId}")
    int updateMastery(LearnerKnowledgeState state);

    /**
     * 删除某个用户的所有画像数据（用于重置）
     */
    @Delete("DELETE FROM learner_knowledge_state WHERE user_id = #{userId}")
    int deleteByUserId(Long userId);
}
