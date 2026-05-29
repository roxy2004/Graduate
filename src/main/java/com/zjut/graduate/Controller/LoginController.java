package com.zjut.graduate.Controller;

import com.zjut.graduate.Po.User;
import com.zjut.graduate.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
/** 登录与登出接口 */
@RequestMapping("/xwd")
public class LoginController {
    @Autowired
    private UserService userService;

    /** 用户登录，校验密码并写入 Session */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginData, HttpSession session) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        Map<String, Object> response = new HashMap<>();
        User user = userService.validateUser(username, password);

        if (user != null) {
            session.setAttribute("userId", user.getId());
            session.setAttribute("role", user.getRole());
            response.put("status", "success");
            response.put("message", "登录成功");
            response.put("user", sanitizeUser(user));
        } else {
            response.put("status", "error");
            response.put("message", "用户名或密码错误");
        }
        return response;
    }

    /** 退出登录，销毁 Session */
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        session.invalidate();
        response.put("status", "success");
        response.put("message", "退出成功");
        return response;
    }

    /** 脱敏用户信息，供前端展示 */
    private Map<String, Object> sanitizeUser(User user) {
        Map<String, Object> safeUser = new HashMap<>();
        safeUser.put("id", user.getId());
        safeUser.put("username", user.getUsername());
        safeUser.put("role", user.getRole());
        safeUser.put("avatarUrl", user.getAvatarUrl());
        safeUser.put("createdAt", user.getCreatedAt());
        return safeUser;
    }
}