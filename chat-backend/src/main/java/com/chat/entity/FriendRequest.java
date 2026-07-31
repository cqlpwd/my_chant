package com.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友请求实体
 */
@Data
@TableName("t_friend_request")
public class FriendRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发送者ID */
    private Long senderId;

    /** 接收者ID */
    private Long receiverId;

    /** 验证消息 */
    private String message;

    /** 状态：0-待处理 1-已同意 2-已拒绝 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
