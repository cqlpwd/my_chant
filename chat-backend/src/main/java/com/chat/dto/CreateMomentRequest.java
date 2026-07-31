package com.chat.dto;

import lombok.Data;

/**
 * 发布动态请求
 */
@Data
public class CreateMomentRequest {

    /** 文字内容 */
    private String content;

    /** 图片URL列表（逗号分隔） */
    private String images;

    /** 位置信息 */
    private String location;
}
