package com.chat.controller;

import com.chat.dto.ApiResponse;
import com.chat.dto.UpdateProfileRequest;
import com.chat.entity.User;
import com.chat.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户控制器：个人信息、搜索用户
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> getCurrentUser(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        User user = userService.getById(userId);
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }

        Map<String, Object> data = new HashMap();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("phone", user.getPhone());
        data.put("email", user.getEmail());
        data.put("gender", user.getGender());
        data.put("signature", user.getSignature());
        data.put("status", user.getStatus());
        return ApiResponse.success(data);
    }

    /**
     * 修改个人信息
     */
    @PutMapping("/profile")
    public ApiResponse<Map<String, Object>> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        try {
            User user = userService.updateProfile(userId, request);
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("username", user.getUsername());
            data.put("nickname", user.getNickname());
            data.put("avatar", user.getAvatar());
            data.put("phone", user.getPhone());
            data.put("email", user.getEmail());
            data.put("gender", user.getGender());
            data.put("signature", user.getSignature());
            return ApiResponse.success("修改成功", data);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 搜索用户（用于添加好友）
     */
    @GetMapping("/search")
    public ApiResponse<List<Map<String, Object>>> searchUsers(
            Authentication authentication,
            @RequestParam String keyword) {
        Long userId = (Long) authentication.getPrincipal();
        List<User> users = userService.searchUsers(keyword, userId);

        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("nickname", u.getNickname());
            map.put("avatar", u.getAvatar());
            map.put("phone", u.getPhone());
            map.put("gender", u.getGender());
            map.put("signature", u.getSignature());
            return map;
        }).collect(Collectors.toList());

        return ApiResponse.success(result);
    }

    /**
     * 获取指定用户信息
     */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getUserById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("gender", user.getGender());
        data.put("signature", user.getSignature());
        data.put("status", user.getStatus());
        return ApiResponse.success(data);
    }
}
