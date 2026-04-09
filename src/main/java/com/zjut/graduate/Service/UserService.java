package com.zjut.graduate.Service;

import com.zjut.graduate.Po.User;

import java.util.List;

public interface UserService {
    User validateUser(String username, String password);

    List<User> listStudents();

    User createStudent(String username, String rawPassword);

    boolean resetPassword(Long userId, String rawPassword);

    boolean deleteStudent(Long userId);
}