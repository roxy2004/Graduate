package com.zjut.graduate.Service;

import com.zjut.graduate.Po.User;

import java.util.List;

public interface UserService {
    User validateUser(String username, String password);

    User getById(Long userId);

    List<User> listStudents();

    User createStudent(String username, String rawPassword);

    boolean resetPassword(Long userId, String rawPassword);

    boolean changePassword(Long userId, String oldRawPassword, String newRawPassword);

    boolean updateAvatar(Long userId, String avatarUrl);

    boolean deleteStudent(Long userId);
}