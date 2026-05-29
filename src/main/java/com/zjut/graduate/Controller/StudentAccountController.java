package com.zjut.graduate.Controller;

import com.zjut.graduate.Po.User;
import com.zjut.graduate.Service.StudentLearningTrendService;
import com.zjut.graduate.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
/** 学生个人中心（资料、密码、头像、趋势） */
@RequestMapping("/xwd/student/account")
public class StudentAccountController {

    @Autowired
    private UserService userService;

    @Autowired
    private StudentLearningTrendService studentLearningTrendService;

    /** 查询个人中心基本信息 */
    @GetMapping("/profile")
    public Map<String, Object> profile(HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        if (user == null) {
            return error("用户不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("role", user.getRole());
        data.put("avatarUrl", user.getAvatarUrl());
        data.put("createdAt", user.getCreatedAt());
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    /** 查询学习趋势图表数据 */
    @GetMapping("/learning-trends")
    public Map<String, Object> learningTrends(HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        Long userId = (Long) session.getAttribute("userId");
        Map<String, Object> data = studentLearningTrendService.buildTrendPayload(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    /** 修改登录密码 */
    @PostMapping("/password")
    public Map<String, Object> changePassword(@RequestBody Map<String, String> body, HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        String oldPassword = body == null ? null : body.get("oldPassword");
        String newPassword = body == null ? null : body.get("newPassword");
        if (isBlank(oldPassword) || isBlank(newPassword)) {
            return error("请输入旧密码和新密码");
        }
        Long userId = (Long) session.getAttribute("userId");
        boolean ok = userService.changePassword(userId, oldPassword.trim(), newPassword.trim());
        if (!ok) {
            return error("旧密码不正确");
        }
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "密码修改成功");
        return response;
    }

    /** 上传并更新头像 */
    @PostMapping("/avatar")
    public Map<String, Object> uploadAvatar(@RequestParam("file") MultipartFile file, HttpSession session) {
        Map<String, Object> authError = requireStudent(session);
        if (authError != null) return authError;
        if (file == null || file.isEmpty()) {
            return error("请上传头像文件");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0 && dot < original.length() - 1) {
            ext = original.substring(dot + 1).toLowerCase();
        }
        if (!("png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext) || "webp".equals(ext))) {
            return error("仅支持 png/jpg/jpeg/webp 格式");
        }
        try {
            Path imagesDir = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "images");
            Files.createDirectories(imagesDir);
            Long userId = (Long) session.getAttribute("userId");
            User user = userService.getById(userId);
            if (user == null) {
                return error("用户不存在");
            }
            String username = sanitizeForFileName(user.getUsername());
            String baseName = userId + "_" + username;
            // 清理该学生旧头像（支持历史不同扩展名），避免目录累积垃圾图片。
            File[] oldFiles = imagesDir.toFile().listFiles((dir, name) ->
                    name != null && name.startsWith(baseName + "."));
            if (oldFiles != null) {
                for (File old : oldFiles) {
                    if (old != null && old.isFile() && !old.delete()) {
                        // 删除失败时继续覆盖写入，不阻塞上传流程。
                    }
                }
            }
            String fileName = baseName + "." + ext;
            Path target = imagesDir.resolve(fileName);
            file.transferTo(target.toFile());
            String avatarUrl = "/xwd/student/account/avatar/" + fileName;
            userService.updateAvatar(userId, avatarUrl);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "头像上传成功");
            response.put("avatarUrl", avatarUrl);
            return response;
        } catch (IOException e) {
            return error("头像上传失败");
        }
    }

    /** 按文件名读取头像静态资源 */
    @GetMapping("/avatar/{fileName:.+}")
    public ResponseEntity<Resource> avatar(@PathVariable("fileName") String fileName) {
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return ResponseEntity.notFound().build();
        }
        Path file = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "images", fileName);
        File f = file.toFile();
        if (!f.exists() || !f.isFile()) {
            return ResponseEntity.notFound().build();
        }
        String lower = fileName.toLowerCase();
        MediaType mt = MediaType.APPLICATION_OCTET_STREAM;
        if (lower.endsWith(".png")) mt = MediaType.IMAGE_PNG;
        else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) mt = MediaType.IMAGE_JPEG;
        else if (lower.endsWith(".webp")) mt = MediaType.valueOf("image/webp");
        return ResponseEntity.ok().contentType(mt).body(new FileSystemResource(f));
    }

    /** 校验学生权限 */
    private Map<String, Object> requireStudent(HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"student".equals(role)) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("code", 403);
            response.put("message", "仅学生可操作");
            return response;
        }
        return null;
    }

    /** 构造错误响应 */
    private Map<String, Object> error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        return response;
    }

    /** 判断字符串是否为空 */
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** 用户名转安全文件名片段 */
    private static String sanitizeForFileName(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) {
            return "student";
        }
        // 只保留安全字符，其他字符统一替换为下划线。
        s = s.replaceAll("[^a-zA-Z0-9_-]", "_");
        // 防止文件名过长
        if (s.length() > 48) {
            s = s.substring(0, 48);
        }
        return s.isEmpty() ? "student" : s;
    }
}
