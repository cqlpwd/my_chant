package com.chat.controller;

import com.chat.dto.ApiResponse;
import com.chat.service.CaptchaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 验证码接口
 */
@RestController
@RequestMapping("/api/auth")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    /**
     * 获取数学验证码
     * 返回：{ captchaKey: "uuid", captchaQuestion: "3 + 7 = ?" }
     */
    @GetMapping("/captcha")
    public ApiResponse<Map<String, String>> getCaptcha() {
        CaptchaService.CaptchaVO vo = captchaService.generate();
        Map<String, String> data = new HashMap<>();
        data.put("captchaKey", vo.getCaptchaKey());
        data.put("captchaQuestion", vo.getCaptchaQuestion());
        return ApiResponse.success(data);
    }
}
