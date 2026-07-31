package com.chat.controller;

import com.chat.dto.ApiResponse;
import com.chat.dto.MessageDto;
import com.chat.entity.Message;
import com.chat.entity.User;
import com.chat.mapper.UserMapper;
import com.chat.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息控制器：聊天记录、会话列表、未读消息、发送消息
 */
@RestController
@RequestMapping("/api/message")
public class MessageController {

    private static final Logger log = LoggerFactory.getLogger(MessageController.class);

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserMapper userMapper;

    public MessageController(MessageService messageService,
                              SimpMessagingTemplate messagingTemplate,
                              UserMapper userMapper) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
        this.userMapper = userMapper;
    }

    /**
     * HTTP 发送消息（保底方案，同时通过 WebSocket 推送给双方）
     */
    @PostMapping("/send")
    public ApiResponse<Map<String, Object>> sendMessage(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {
        Long senderId = (Long) authentication.getPrincipal();
        Long receiverId = Long.valueOf(body.get("receiverId").toString());
        String content = (String) body.get("content");
        Integer messageType = body.containsKey("messageType")
                ? Integer.valueOf(body.get("messageType").toString()) : 0;

        try {
            Message message = messageService.sendMessage(senderId, receiverId, content, messageType);

            User sender = userMapper.selectById(senderId);

            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "CHAT");
            payload.put("id", message.getId());
            payload.put("senderId", senderId);
            payload.put("senderNickname", sender != null ? sender.getNickname() : "");
            payload.put("senderAvatar", sender != null ? sender.getAvatar() : "");
            payload.put("receiverId", message.getReceiverId());
            payload.put("content", message.getContent());
            payload.put("messageType", message.getMessageType());
            payload.put("status", message.getStatus());
            payload.put("createdAt", message.getCreatedAt() != null
                    ? message.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // WebSocket 推送给接收方
            messagingTemplate.convertAndSendToUser(
                    receiverId.toString(), "/queue/chat", payload);
            log.info("[HTTP] 已推送给接收者: user={}, msgId={}", receiverId, message.getId());
            // 回执给发送方
            messagingTemplate.convertAndSendToUser(
                    senderId.toString(), "/queue/chat", payload);
            log.info("[HTTP] 已推送回执给发送者: user={}, msgId={}", senderId, message.getId());

            return ApiResponse.success("发送成功", payload);
        } catch (RuntimeException e) {
            log.warn("[HTTP] 发送失败: sender={}, receiver={}, error={}", senderId, receiverId, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取与某个好友的聊天记录
     */
    @GetMapping("/history/{friendId}")
    public ApiResponse<List<MessageDto>> getChatHistory(
            Authentication authentication,
            @PathVariable Long friendId) {
        Long userId = (Long) authentication.getPrincipal();
        List<MessageDto> messages = messageService.getChatHistory(userId, friendId, userId);
        return ApiResponse.success(messages);
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    public ApiResponse<List<Map<String, Object>>> getConversations(
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<Map<String, Object>> conversations = messageService.getConversationList(userId);
        return ApiResponse.success(conversations);
    }

    /**
     * 标记消息为已读
     */
    @PutMapping("/read/{friendId}")
    public ApiResponse<Void> markAsRead(
            Authentication authentication,
            @PathVariable Long friendId) {
        Long userId = (Long) authentication.getPrincipal();
        messageService.markAsRead(userId, friendId);
        return ApiResponse.success("已读", null);
    }

    /**
     * 获取未读消息数
     */
    @GetMapping("/unread")
    public ApiResponse<Map<String, Integer>> getUnreadCount(
            Authentication authentication,
            @RequestParam(required = false) Long friendId) {
        Long userId = (Long) authentication.getPrincipal();
        Map<String, Integer> unread = messageService.getUnreadCount(userId, friendId);
        return ApiResponse.success(unread);
    }
}
