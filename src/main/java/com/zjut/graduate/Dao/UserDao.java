package com.zjut.graduate.Dao;

import com.zjut.graduate.Po.User;
import org.apache.ibatis.annotations.*;
@Mapper
public interface UserDao {
    @Select("SELECT * FROM user WHERE id = #{id}")
    @Results(id = "userResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "username", property = "username"),
            @Result(column = "password", property = "password"),
            @Result(column = "role", property = "role"),
            @Result(column = "created_at", property = "createdAt")
    })
    User selectById(Long id);

    @Select("SELECT * FROM user WHERE username = #{username}")
    @ResultMap("userResult")
    User selectByUsername(String username);

    @Insert("INSERT INTO user (username, password, role, created_at) VALUES (#{username}, #{password}, #{role}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE user SET username = #{username}, password = #{password}, role = #{role}, created_at = #{createdAt} WHERE id = #{id}")
    int update(User user);

    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteById(Long id);
}
