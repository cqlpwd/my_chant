package com.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_group")
public class Group {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String avatar;
    private Long ownerId;
    private String announcement;
    private Integer memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
