-- ============================================
-- Mychant 应用 - 数据库初始化脚本
-- MySQL 8.0+
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS chat_app
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE chat_app;

-- ============================================
-- 用户表
-- ============================================
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名（登录账号）',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    avatar VARCHAR(500) DEFAULT '/uploads/default-avatar.svg' COMMENT '头像URL',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    gender TINYINT DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女',
    signature VARCHAR(200) COMMENT '个性签名',
    status TINYINT DEFAULT 0 COMMENT '在线状态：0-离线 1-在线',
    last_login_time DATETIME COMMENT '最后登录时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_nickname (nickname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 消息表
-- ============================================
DROP TABLE IF EXISTS t_message;
CREATE TABLE t_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者ID',
    content TEXT NOT NULL COMMENT '消息内容',
    message_type TINYINT DEFAULT 0 COMMENT '消息类型：0-文本 1-图片 2-语音 3-视频 4-文件',
    status TINYINT DEFAULT 0 COMMENT '消息状态：0-未读 1-已读',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    INDEX idx_sender_receiver (sender_id, receiver_id),
    INDEX idx_receiver_sender (receiver_id, sender_id),
    INDEX idx_receiver_status (receiver_id, status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ============================================
-- 好友关系表
-- ============================================
DROP TABLE IF EXISTS t_friendship;
CREATE TABLE t_friendship (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关系ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    friend_id BIGINT NOT NULL COMMENT '好友ID',
    remark VARCHAR(50) COMMENT '好友备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    UNIQUE KEY uk_user_friend (user_id, friend_id),
    INDEX idx_user_id (user_id),
    INDEX idx_friend_id (friend_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

-- ============================================
-- 好友请求表
-- ============================================
DROP TABLE IF EXISTS t_friend_request;
CREATE TABLE t_friend_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '请求ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者ID',
    message VARCHAR(200) DEFAULT '请求添加您为好友' COMMENT '验证消息',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待处理 1-已同意 2-已拒绝',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '处理时间',
    INDEX idx_receiver_status (receiver_id, status),
    INDEX idx_sender_receiver (sender_id, receiver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友请求表';

-- ============================================
-- 插入测试用户（密码都是 123456，BCrypt 加密后）
-- 实际使用中请通过注册接口创建用户
-- ============================================
-- 密码 123456 的 BCrypt 密文
INSERT INTO t_user (username, password, nickname, avatar, gender, signature) VALUES
('zhangsan', '$2a$10$VqveusjLdoWPZw.oi.eP/.jFced2jt221idfl2G4WM0Nr7Bc5DdNu', '张三', '/uploads/default-avatar.svg', 1, 'Hello, 我是张三'),
('lisi', '$2a$10$VqveusjLdoWPZw.oi.eP/.jFced2jt221idfl2G4WM0Nr7Bc5DdNu', '李四', '/uploads/default-avatar.svg', 2, '今天也是开心的一天'),
('wangwu', '$2a$10$VqveusjLdoWPZw.oi.eP/.jFced2jt221idfl2G4WM0Nr7Bc5DdNu', '王五', '/uploads/default-avatar.svg', 1, '天天向上');

-- ============================================
-- 朋友圈动态表
-- ============================================
DROP TABLE IF EXISTS t_moment;
CREATE TABLE t_moment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '动态ID',
    user_id BIGINT NOT NULL COMMENT '发布者ID',
    content TEXT COMMENT '文字内容',
    images TEXT COMMENT '图片URL列表，JSON数组格式',
    location VARCHAR(200) COMMENT '位置信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='朋友圈动态表';

-- ============================================
-- 朋友圈点赞表
-- ============================================
DROP TABLE IF EXISTS t_moment_like;
CREATE TABLE t_moment_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞ID',
    moment_id BIGINT NOT NULL COMMENT '动态ID',
    user_id BIGINT NOT NULL COMMENT '点赞用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    UNIQUE KEY uk_moment_user (moment_id, user_id),
    INDEX idx_moment_id (moment_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='朋友圈点赞表';

-- ============================================
-- 朋友圈评论表
-- ============================================
DROP TABLE IF EXISTS t_moment_comment;
CREATE TABLE t_moment_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
    moment_id BIGINT NOT NULL COMMENT '动态ID',
    user_id BIGINT NOT NULL COMMENT '评论者ID',
    content VARCHAR(500) NOT NULL COMMENT '评论内容',
    reply_to BIGINT COMMENT '回复的评论ID，NULL表示直接评论动态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    INDEX idx_moment_id (moment_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='朋友圈评论表';

-- ============================================
-- 群聊 - 群组表
-- ============================================
DROP TABLE IF EXISTS t_group;
CREATE TABLE t_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '群ID',
    name VARCHAR(100) NOT NULL COMMENT '群名称',
    avatar VARCHAR(500) DEFAULT '/uploads/default-group.svg' COMMENT '群头像',
    owner_id BIGINT NOT NULL COMMENT '群主ID',
    announcement VARCHAR(500) COMMENT '群公告',
    member_count INT DEFAULT 1 COMMENT '成员数量',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_owner_id (owner_id),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组表';

-- ============================================
-- 群聊 - 群成员表
-- ============================================
DROP TABLE IF EXISTS t_group_member;
CREATE TABLE t_group_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    group_id BIGINT NOT NULL COMMENT '群ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role TINYINT DEFAULT 0 COMMENT '角色：0-成员 1-管理员 2-群主',
    nickname_in_group VARCHAR(50) COMMENT '群内昵称',
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    UNIQUE KEY uk_group_user (group_id, user_id),
    INDEX idx_user_id (user_id),
    INDEX idx_group_id (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';

-- ============================================
-- 群聊 - 群消息表
-- ============================================
DROP TABLE IF EXISTS t_group_message;
CREATE TABLE t_group_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    group_id BIGINT NOT NULL COMMENT '群ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    content TEXT NOT NULL COMMENT '消息内容',
    message_type TINYINT DEFAULT 0 COMMENT '消息类型：0-文本 1-图片 2-语音 3-视频 4-文件',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    INDEX idx_group_id_created (group_id, created_at),
    INDEX idx_sender_id (sender_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群消息表';
