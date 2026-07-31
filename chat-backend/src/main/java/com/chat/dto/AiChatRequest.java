package com.chat.dto;

import lombok.Data;
import java.util.List;

/**
 * AI 聊天请求
 */
@Data
public class AiChatRequest {

    /** 对话消息列表，每条包含 role 和 content */
    private List<Message> messages;

    @Data
    public static class Message {
        private String role;
        private String content;
    }
}
