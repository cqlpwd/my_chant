package com.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 朋友圈动态实体
 */
@Data
@TableName("t_moment")
public class Moment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发布者ID */
    private Long userId;

    /** 文字内容 */
    private String content;

    /** 图片URL列表（JSON数组） */
    private String images;

    /** 位置信息 */
    private String location;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
