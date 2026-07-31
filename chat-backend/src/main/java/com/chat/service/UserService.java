package com.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chat.dto.LoginRequest;
import com.chat.dto.RegisterRequest;
import com.chat.dto.UpdateProfileRequest;
import com.chat.entity.User;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    User register(RegisterRequest request);

    /**
     * 用户登录：返回 JWT Token
     */
    String login(LoginRequest request);

    /**
     * 根据用户名查找用户
     */
    User findByUsername(String username);

    /**
     * 根据手机号查找用户
     */
    User findByPhone(String phone);

    /**
     * 搜索用户
     */
    List<User> searchUsers(String keyword, Long currentUserId);

    /**
     * 修改个人信息
     */
    User updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * 获取好友ID列表
     */
    List<Long> getFriendIds(Long userId);
}
