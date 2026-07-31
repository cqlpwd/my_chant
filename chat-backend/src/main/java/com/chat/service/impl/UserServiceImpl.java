package com.chat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.dto.LoginRequest;
import com.chat.dto.RegisterRequest;
import com.chat.dto.UpdateProfileRequest;
import com.chat.entity.Friendship;
import com.chat.entity.User;
import com.chat.mapper.FriendshipMapper;
import com.chat.mapper.UserMapper;
import com.chat.service.CaptchaService;
import com.chat.service.UserService;
import com.chat.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户服务实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final FriendshipMapper friendshipMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CaptchaService captchaService;

    public UserServiceImpl(UserMapper userMapper,
                           FriendshipMapper friendshipMapper,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           CaptchaService captchaService) {
        this.userMapper = userMapper;
        this.friendshipMapper = friendshipMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.captchaService = captchaService;
    }

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        // 校验验证码
        if (!captchaService.verify(request.getCaptchaKey(), request.getCaptchaCode())) {
            throw new RuntimeException("验证码错误或已过期");
        }

        // 检查用户名是否已存在
        User existUser = userMapper.findByUsername(request.getUsername());
        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查手机号是否已注册
        User existPhone = userMapper.findByPhone(request.getPhone());
        if (existPhone != null) {
            throw new RuntimeException("该手机号已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setAvatar("/uploads/default-avatar.svg");
        user.setStatus(0);
        user.setGender(0);
        userMapper.insert(user);
        return user;
    }

    @Override
    public String login(LoginRequest request) {
        User user = userMapper.findByPhone(request.getPhone());
        if (user == null) {
            throw new RuntimeException("手机号或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("手机号或密码错误");
        }

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 生成 JWT Token
        return jwtUtil.generateToken(user.getId(), user.getUsername());
    }

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public User findByPhone(String phone) {
        return userMapper.findByPhone(phone);
    }

    @Override
    public List<User> searchUsers(String keyword, Long currentUserId) {
        return userMapper.searchUsers(keyword, currentUserId);
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getSignature() != null) {
            user.setSignature(request.getSignature());
        }

        userMapper.updateById(user);
        return userMapper.selectById(userId);
    }

    @Override
    public List<Long> getFriendIds(Long userId) {
        return userMapper.getFriendIds(userId);
    }
}
