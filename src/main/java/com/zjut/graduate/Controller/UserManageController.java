package com.zjut.graduate.Controller;

import com.zjut.graduate.Po.User;
import com.zjut.graduate.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/xwd/users")
public class UserManageController {

    @Autowired
    private UserService userService;

    @GetMapping("/students")
    public Map<String, Object> listStudents(HttpSession session) {
        Map<String, Object> authError = requireTeacher(session);
        if (authError != null) {
            return authError;
        }
        List<Map<String, Object>> students = userService.listStudents()
                .stream()
                .map(this::sanitizeUser)
                .collect(Collectors.toList());
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", students);
        return response;
    }

    @PostMapping("/students")
    public Map<String, Object> createStudent(@RequestBody Map<String, String> payload, HttpSession session) {
        Map<String, Object> authError = requireTeacher(session);
        if (authError != null) {
            return authError;
        }
        String username = payload.get("username");
        String password = payload.get("password");
        if (isBlank(username) || isBlank(password)) {
            return error("用户名和密码不能为空");
        }
        User createdUser = userService.createStudent(username.trim(), password.trim());
        if (createdUser == null) {
            return error("用户名已存在");
        }
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "创建学生账号成功");
        response.put("data", sanitizeUser(createdUser));
        return response;
    }

    @PutMapping("/students/{id}/password")
    public Map<String, Object> resetStudentPassword(@PathVariable("id") Long id,
                                                    @RequestBody Map<String, String> payload,
                                                    HttpSession session) {
        Map<String, Object> authError = requireTeacher(session);
        if (authError != null) {
            return authError;
        }
        String password = payload.get("password");
        if (isBlank(password)) {
            return error("新密码不能为空");
        }
        boolean updated = userService.resetPassword(id, password.trim());
        if (!updated) {
            return error("更新密码失败，学生不存在");
        }
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "密码重置成功");
        return response;
    }

    @DeleteMapping("/students/{id}")
    public Map<String, Object> deleteStudent(@PathVariable("id") Long id, HttpSession session) {
        Map<String, Object> authError = requireTeacher(session);
        if (authError != null) {
            return authError;
        }
        boolean deleted = userService.deleteStudent(id);
        if (!deleted) {
            return error("删除失败，学生不存在");
        }
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "删除学生成功");
        return response;
    }

    private Map<String, Object> requireTeacher(HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"teacher".equals(role)) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("code", 403);
            response.put("message", "仅教师可操作");
            return response;
        }
        return null;
    }

    private Map<String, Object> sanitizeUser(User user) {
        Map<String, Object> safeUser = new HashMap<>();
        safeUser.put("id", user.getId());
        safeUser.put("username", user.getUsername());
        safeUser.put("role", user.getRole());
        safeUser.put("createdAt", user.getCreatedAt());
        return safeUser;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        return response;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
