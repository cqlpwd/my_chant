package com.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_group_message")
public class GroupMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long senderId;
    private String content;
    /** 0-文本 1-图片 2-语音 3-视频 4-文件 */
    private Integer messageType;
    private LocalDateTime createdAt;
}
