package com.zjut.graduate.Service.impl;

import com.zjut.graduate.Dao.UserDao;
import com.zjut.graduate.Po.User;
import com.zjut.graduate.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User validateUser(String username, String password) {
        User user = userDao.selectByUsername(username);
        if (user == null || user.getPassword() == null) {
            return null;
        }
        String storedPassword = user.getPassword();

        // 兼容历史明文密码：首次登录成功后自动升级为 bcrypt。
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(password, storedPassword) ? user : null;
        }

        if (storedPassword.equals(password)) {
            userDao.updatePasswordById(user.getId(), passwordEncoder.encode(password));
            return user;
        }
        return null;
    }

    @Override
    public List<User> listStudents() {
        return userDao.selectByRole("student");
    }

    @Override
    public User createStudent(String username, String rawPassword) {
        if (userDao.countByUsername(username) > 0) {
            return null;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole("student");
        user.setCreatedAt(new Date());
        userDao.insert(user);
        return user;
    }

    @Override
    public boolean resetPassword(Long userId, String rawPassword) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        return userDao.updatePasswordById(userId, encodedPassword) > 0;
    }

    @Override
    public boolean deleteStudent(Long userId) {
        User user = userDao.selectById(userId);
        if (user == null || !"student".equals(user.getRole())) {
            return false;
        }
        return userDao.deleteById(userId) > 0;
    }
}