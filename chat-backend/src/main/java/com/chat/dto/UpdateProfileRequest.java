package com.chat.dto;

import lombok.Data;

/**
 * 修改个人信息请求 DTO
 */
@Data
public class UpdateProfileRequest {

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 性别：0-未知 1-男 2-女 */
    private Integer gender;

    /** 个性签名 */
    private String signature;
}
