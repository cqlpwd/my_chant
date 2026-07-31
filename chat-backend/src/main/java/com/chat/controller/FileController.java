package com.chat.controller;

import com.chat.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器：头像、聊天图片
 */
@RestController
@RequestMapping("/api/upload")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "uploads";

    /**
     * 上传图片（聊天图片、头像等）
     */
    @PostMapping("/image")
    public ApiResponse<Map<String, Object>> uploadImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        Long userId = (Long) authentication.getPrincipal();

        if (file.isEmpty()) {
            return ApiResponse.error(400, "文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ApiResponse.error(400, "仅支持图片格式（jpg、png、gif、webp）");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            return ApiResponse.error(400, "图片大小不能超过 5MB");
        }

        try {
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String originalName = file.getOriginalFilename();
            String ext = ".png";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
                // 统一非标准后缀
                if (!ext.matches("\\.(jpg|jpeg|png|gif|webp|bmp)$")) {
                    ext = ".png";
                }
            }

            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
            Path filePath = Paths.get(UPLOAD_DIR, fileName);
            Files.write(filePath, file.getBytes());

            String url = "/uploads/" + fileName;
            log.info("图片上传成功: userId={}, url={}", userId, url);

            Map<String, Object> data = new HashMap<>();
            data.put("url", url);
            return ApiResponse.success("上传成功", data);
        } catch (IOException e) {
            log.error("图片上传失败: userId={}", userId, e);
            return ApiResponse.error(500, "上传失败，请重试");
        }
    }
}
