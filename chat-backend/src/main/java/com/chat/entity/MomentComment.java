package com.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 朋友圈评论实体
 */
@Data
@TableName("t_moment_comment")
public class MomentComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 动态ID */
    private Long momentId;

    /** 评论者ID */
    private Long userId;

    /** 评论内容 */
    private String content;

    /** 回复的评论ID，NULL表示直接评论动态 */
    private Long replyTo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
