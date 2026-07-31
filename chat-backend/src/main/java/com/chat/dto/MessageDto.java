package com.chat.dto;

import lombok.Data;

/**
 * 消息 DTO
 */
@Data
public class MessageDto {

    /** 消息ID */
    private Long id;

    /** 发送者ID */
    private Long senderId;

    /** 发送者昵称 */
    private String senderNickname;

    /** 发送者头像 */
    private String senderAvatar;

    /** 接收者ID */
    private Long receiverId;

    /** 消息内容 */
    private String content;

    /** 消息类型：0-文本 1-图片 2-语音 3-视频 4-文件 */
    private Integer messageType;

    /** 消息状态：0-未读 1-已读 */
    private Integer status;

    /** 发送时间 */
    private String createdAt;
}
