package com.zjut.graduate.Controller;

import com.zjut.graduate.Po.User;
import com.zjut.graduate.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/xwd")
public class LoginController {
    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        Map<String, Object> response = new HashMap<>();
        User user = userService.validateUser(username, password);

        if (user != null) {
            response.put("status", "success");
            response.put("message", "登录成功");
            response.put("user", user);
        } else {
            response.put("status", "error");
            response.put("message", "用户名或密码错误");
        }
        return response;
    }
}