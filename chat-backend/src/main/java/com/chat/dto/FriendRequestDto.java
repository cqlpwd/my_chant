package com.chat.dto;

import lombok.Data;

/**
 * 好友请求 DTO
 */
@Data
public class FriendRequestDto {

    /** 好友请求ID */
    private Long id;

    /** 发送者ID */
    private Long senderId;

    /** 发送者用户名 */
    private String senderUsername;

    /** 发送者昵称 */
    private String senderNickname;

    /** 发送者头像 */
    private String senderAvatar;

    /** 接收者ID */
    private Long receiverId;

    /** 验证消息 */
    private String message;

    /** 状态 */
    private Integer status;

    /** 发送时间 */
    private String createdAt;
}
