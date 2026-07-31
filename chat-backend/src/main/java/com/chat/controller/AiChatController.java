package com.chat.controller;

import com.chat.dto.AiChatRequest;
import com.chat.dto.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * AI 聊天控制器（DeepSeek）
 */
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.api-url}")
    private String apiUrl;

    @Value("${deepseek.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        log.info("AI 配置加载 - apiUrl: {}, model: {}, apiKey 已配置: {} (长度: {})",
                apiUrl, model, !apiKey.isBlank() && !apiKey.startsWith("your-"), apiKey.length());
    }

    @PostMapping("/chat")
    public ApiResponse<Map<String, Object>> chat(@RequestBody AiChatRequest request,
                                                  Authentication authentication) {
        // 检查 API Key 是否已配置
        if (apiKey.isBlank() || apiKey.startsWith("your-")) {
            return ApiResponse.error(500, "API Key 未配置，请在 application.yml 中设置 deepseek.api-key");
        }

        try {
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", "你是一个友好、有帮助的AI助手。请用简洁清晰的中文回答用户的问题。");
            messages.add(systemMsg);

            for (AiChatRequest.Message msg : request.getMessages()) {
                Map<String, String> m = new HashMap<>();
                m.put("role", msg.getRole());
                m.put("content", msg.getContent());
                messages.add(m);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", messages);
            body.put("stream", false);
            body.put("temperature", 0.7);
            body.put("max_tokens", 2048);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            log.info("请求 AI API - URL: {}, model: {}, 消息数: {}", apiUrl, model, messages.size());
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, String.class);

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            JsonNode choices = jsonNode.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                String reply = choices.get(0).get("message").get("content").asText();
                log.info("AI 响应成功, 回复长度: {}", reply.length());

                Map<String, Object> result = new HashMap<>();
                result.put("reply", reply);
                return ApiResponse.success(result);
            }

            log.warn("AI 响应无有效 choices, 完整响应: {}", response.getBody());
            return ApiResponse.error("AI 未返回有效响应");

        } catch (HttpClientErrorException e) {
            String respBody = e.getResponseBodyAsString();
            log.error("AI API 错误 - URL: {}, 状态码: {}, 响应体: {}", apiUrl, e.getStatusCode(), respBody);
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return ApiResponse.error(401, "API Key 认证失败，请检查: 1) api-key 是否正确 2) api-url 是否匹配密钥来源");
            }
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                return ApiResponse.error(429, "请求过于频繁，请稍后重试");
            }
            return ApiResponse.error(e.getStatusCode().value(), "AI 服务错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("AI 调用异常 - URL: {}", apiUrl, e);
            return ApiResponse.error("AI 服务异常: " + e.getMessage());
        }
    }
}
