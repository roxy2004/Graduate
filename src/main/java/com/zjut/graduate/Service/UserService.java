package com.zjut.graduate.Service;

import com.zjut.graduate.Po.User;

import java.util.List;

/** 用户账号与认证 */
public interface UserService {

    /** 校验用户名密码，成功返回用户 */
    User validateUser(String username, String password);

    /** 按 id 查询用户 */
    User getById(Long userId);

    /** 查询全部学生账号 */
    List<User> listStudents();

    /** 创建学生账号 */
    User createStudent(String username, String rawPassword);

    /** 重置密码（教师操作） */
    boolean resetPassword(Long userId, String rawPassword);

    /** 修改密码（需校验旧密码） */
    boolean changePassword(Long userId, String oldRawPassword, String newRawPassword);

    /** 更新头像 URL */
    boolean updateAvatar(Long userId, String avatarUrl);

    /** 删除学生账号 */
    boolean deleteStudent(Long userId);
}
