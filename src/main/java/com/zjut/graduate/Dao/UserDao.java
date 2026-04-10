package com.zjut.graduate.Dao;

import com.zjut.graduate.Po.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserDao {
    @Select("SELECT * FROM user WHERE id = #{id}")
    @Results(id = "userResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "username", property = "username"),
            @Result(column = "password_hash", property = "password"),
            @Result(column = "role", property = "role"),
            @Result(column = "created_at", property = "createdAt")
    })
    User selectById(Long id);

    @Select("SELECT * FROM user WHERE username = #{username}")
    @ResultMap("userResult")
    User selectByUsername(String username);

    @Select("SELECT * FROM user WHERE role = #{role} ORDER BY created_at DESC")
    @ResultMap("userResult")
    List<User> selectByRole(String role);

    @Select("SELECT COUNT(1) FROM user WHERE username = #{username}")
    int countByUsername(String username);

    @Insert("INSERT INTO user (username, password_hash, role, status, created_at, updated_at) " +
            "VALUES (#{username}, #{password}, #{role}, 1, #{createdAt}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE user SET password_hash = #{password}, updated_at = NOW() WHERE id = #{id}")
    int updatePasswordById(@Param("id") Long id, @Param("password") String password);

    @Update("UPDATE user SET username = #{username}, password_hash = #{password}, role = #{role}, " +
            "created_at = #{createdAt}, updated_at = NOW() WHERE id = #{id}")
    int update(User user);

    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteById(Long id);
}
