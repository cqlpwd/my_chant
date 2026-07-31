package com.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息实体
 */
@Data
@TableName("t_message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发送者ID */
    private Long senderId;

    /** 接收者ID */
    private Long receiverId;

    /** 消息内容 */
    private String content;

    /** 消息类型：0-文本 1-图片 2-语音 3-视频 4-文件 */
    private Integer messageType;

    /** 消息状态：0-未读 1-已读 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
