package com.zjut.graduate.Service;

import com.zjut.graduate.Po.User;

public interface UserService {
    User validateUser(String username, String password);
}