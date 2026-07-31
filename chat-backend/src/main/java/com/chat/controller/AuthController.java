package com.chat.controller;

import com.chat.dto.ApiResponse;
import com.chat.dto.LoginRequest;
import com.chat.dto.RegisterRequest;
import com.chat.entity.User;
import com.chat.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器：登录和注册
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.register(request);
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("username", user.getUsername());
            data.put("nickname", user.getNickname());
            return ApiResponse.success("注册成功", data);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        try {
            String token = userService.login(request);
            User user = userService.findByPhone(request.getPhone());

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", userToMap(user));
            return ApiResponse.success("登录成功", data);
        } catch (RuntimeException e) {
            return ApiResponse.error(401, e.getMessage());
        }
    }

    private Map<String, Object> userToMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname());
        map.put("avatar", user.getAvatar());
        map.put("phone", user.getPhone());
        map.put("email", user.getEmail());
        map.put("gender", user.getGender());
        map.put("signature", user.getSignature());
        map.put("status", user.getStatus());
        return map;
    }
}
