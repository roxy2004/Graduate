package com.zjut.graduate.Dao;

import com.zjut.graduate.Po.KnowledgePoint;
import org.apache.ibatis.annotations.*;

import java.util.List;
@Mapper
public interface KnowledgePointDao {
    @Select("SELECT * FROM knowledge_point WHERE id = #{id}")
    KnowledgePoint selectById(Long id);

    @Select("SELECT * FROM knowledge_point")
    List<KnowledgePoint> selectAll();

    @Insert("INSERT INTO knowledge_point (name) VALUES (#{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgePoint kp);

    @Update("UPDATE knowledge_point SET name = #{name} WHERE id = #{id}")
    int update(KnowledgePoint kp);

    @Delete("DELETE FROM knowledge_point WHERE id = #{id}")
    int deleteById(Long id);
}
