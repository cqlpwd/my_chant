package com.chat.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 注册请求 DTO
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 30, message = "用户名长度应在3-30之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 30, message = "密码长度应在8-30之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
             message = "密码必须同时包含字母和数字")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 30, message = "昵称长度不能超过30")
    private String nickname;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$",
             message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "验证码标识不能为空")
    private String captchaKey;

    private int captchaCode;
}
