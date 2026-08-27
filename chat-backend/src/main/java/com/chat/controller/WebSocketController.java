package com.chat.controller;

import com.chat.entity.GroupMessage;
import com.chat.entity.Message;
import com.chat.entity.User;
import com.chat.mapper.GroupMemberMapper;
import com.chat.mapper.UserMapper;
import com.chat.service.GroupService;
import com.chat.service.MessageService;
import com.chat.websocket.WebSocketAuthInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 消息控制器：处理实时消息
 */
@Controller
public class WebSocketController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final GroupService groupService;
    private final UserMapper userMapper;
    private final GroupMemberMapper groupMemberMapper;

    public WebSocketController(SimpMessagingTemplate messagingTemplate,
                                MessageService messageService,
                                GroupService groupService,
                                UserMapper userMapper,
                                GroupMemberMapper groupMemberMapper) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
        this.groupService = groupService;
        this.userMapper = userMapper;
        this.groupMemberMapper = groupMemberMapper;
    }

    /**
     * 处理点对点聊天消息
     * 客户端发送到：/app/chat.send
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload Map<String, Object> payload, Principal principal) {
        Long senderId = getUserIdFromPrincipal(principal);
        Long receiverId = Long.valueOf(payload.get("receiverId").toString());
        String content = (String) payload.get("content");
        Integer messageType = payload.containsKey("messageType")
                ? Integer.valueOf(payload.get("messageType").toString()) : 0;

        log.info("[WS] 收到消息: sender={}, receiver={}, content={}", senderId, receiverId, content);

        try {
            // 保存消息到数据库
            Message message = messageService.sendMessage(senderId, receiverId, content, messageType);

            // 构建发送给接收者的消息
            Map<String, Object> msgToReceiver = buildMessagePayload(message, senderId);
            // 构建发送给发送者的回执
            Map<String, Object> msgToSender = buildMessagePayload(message, senderId);

            // 发送给接收者（点对点）
            messagingTemplate.convertAndSendToUser(
                    receiverId.toString(),
                    "/queue/chat",
                    msgToReceiver
            );
            log.info("[WS] 已推送给接收者: user={}, dest=/queue/chat, msgId={}", receiverId, message.getId());

            // 同时发送回执给发送者
            messagingTemplate.convertAndSendToUser(
                    senderId.toString(),
                    "/queue/chat",
                    msgToSender
            );
            log.info("[WS] 已推送回执给发送者: user={}, msgId={}", senderId, message.getId());
        } catch (RuntimeException e) {
            log.warn("[WS] 发送失败: sender={}, receiver={}, error={}", senderId, receiverId, e.getMessage());
            // 发送错误消息给发送者
            Map<String, Object> errorMsg = new HashMap<>();
            errorMsg.put("type", "ERROR");
            errorMsg.put("content", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                    senderId.toString(),
                    "/queue/chat",
                    errorMsg
            );
        }
    }

    /**
     * 处理消息已读回执
     * 客户端发送到：/app/chat.read
     */
    @MessageMapping("/chat.read")
    public void markMessageAsRead(@Payload Map<String, Object> payload, Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);
        Long friendId = Long.valueOf(payload.get("friendId").toString());

        messageService.markAsRead(userId, friendId);

        // 通知对方消息已读
        Map<String, Object> readReceipt = new HashMap<>();
        readReceipt.put("type", "READ_RECEIPT");
        readReceipt.put("readerId", userId);

        messagingTemplate.convertAndSendToUser(
                friendId.toString(),
                "/queue/chat",
                readReceipt
        );
    }

    /**
     * 处理"正在输入"状态通知
     * 客户端发送到：/app/chat.typing
     */
    @MessageMapping("/chat.typing")
    public void typingNotification(@Payload Map<String, Object> payload, Principal principal) {
        Long senderId = getUserIdFromPrincipal(principal);
        Long receiverId = Long.valueOf(payload.get("receiverId").toString());

        Map<String, Object> typingMsg = new HashMap<>();
        typingMsg.put("type", "TYPING");
        typingMsg.put("userId", senderId);
        typingMsg.put("typing", payload.getOrDefault("typing", true));

        messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/chat",
                typingMsg
        );
    }

    /**
     * 处理好友请求的 WebSocket 通知
     * 客户端发送到：/app/friend.request
     */
    @MessageMapping("/friend.request")
    public void friendRequestNotification(@Payload Map<String, Object> payload, Principal principal) {
        Long receiverId = Long.valueOf(payload.get("receiverId").toString());

        Map<String, Object> notify = new HashMap<>();
        notify.put("type", "FRIEND_REQUEST");
        notify.put("message", "您有新的好友请求");

        messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/notification",
                notify
        );
    }

    /**
     * 处理视频通话信令（WebRTC），仅做点对点转发，不解析媒体内容
     * 客户端发送到：/app/call.signal
     * payload 示例:
     *   { receiverId, type: CALL_OFFER,   sdp }          发起呼叫（携带 offer）
     *   { receiverId, type: CALL_ANSWER,  sdp }          接听（携带 answer）
     *   { receiverId, type: CALL_ICE,     candidate }    交换 ICE 候选
     *   { receiverId, type: CALL_HANGUP }                挂断
     *   { receiverId, type: CALL_REJECT }                拒绝
     *   { receiverId, type: CALL_BUSY }                  对方忙线
     */
    @MessageMapping("/call.signal")
    public void callSignal(@Payload Map<String, Object> payload, Principal principal) {
        Long senderId = getUserIdFromPrincipal(principal);
        Long receiverId = Long.valueOf(payload.get("receiverId").toString());
        String type = payload.getOrDefault("type", "UNKNOWN").toString();

        Map<String, Object> signal = new HashMap<>(payload);
        signal.put("fromId", senderId);
        signal.put("type", type);
        signal.remove("receiverId"); // 对端只需要知道自己收到的信令类型

        log.info("[WS-通话] 转发信令: {} -> {}, type={}", senderId, receiverId, type);
        messagingTemplate.convertAndSendToUser(receiverId.toString(), "/queue/call", signal);
    }

    /**
     * 处理群聊消息
     * 客户端发送到：/app/group.send
     */
    @MessageMapping("/group.send")
    public void sendGroupMessage(@Payload Map<String, Object> payload, Principal principal) {
        Long senderId = getUserIdFromPrincipal(principal);
        Long groupId = Long.valueOf(payload.get("groupId").toString());
        String content = (String) payload.get("content");
        Integer messageType = payload.containsKey("messageType")
                ? Integer.valueOf(payload.get("messageType").toString()) : 0;

        log.info("[WS-群聊] 收到消息: sender={}, groupId={}, content={}", senderId, groupId, content);

        try {
            GroupMessage msg = groupService.sendGroupMessage(groupId, senderId, content, messageType);
            User sender = userMapper.selectById(senderId);

            Map<String, Object> msgPayload = new HashMap<>();
            msgPayload.put("type", "GROUP_CHAT");
            msgPayload.put("id", msg.getId());
            msgPayload.put("groupId", groupId);
            msgPayload.put("senderId", senderId);
            msgPayload.put("senderNickname", sender != null ? sender.getNickname() : "");
            msgPayload.put("senderAvatar", sender != null ? sender.getAvatar() : "");
            msgPayload.put("content", msg.getContent());
            msgPayload.put("messageType", msg.getMessageType());
            msgPayload.put("createdAt", msg.getCreatedAt() != null
                    ? msg.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // 推送给群内所有成员
            List<Long> memberIds = groupMemberMapper.findMemberIdsByGroupId(groupId);
            log.info("[WS-群聊] 群成员数: {}, groupId={}", memberIds.size(), groupId);
            for (Long memberId : memberIds) {
                messagingTemplate.convertAndSendToUser(memberId.toString(), "/queue/chat", msgPayload);
            }
        } catch (RuntimeException e) {
            log.warn("[WS-群聊] 发送失败: sender={}, groupId={}, error={}", senderId, groupId, e.getMessage());
            Map<String, Object> errorMsg = new HashMap<>();
            errorMsg.put("type", "ERROR");
            errorMsg.put("content", e.getMessage());
            messagingTemplate.convertAndSendToUser(senderId.toString(), "/queue/chat", errorMsg);
        }
    }

    /**
     * 构建消息负载
     */
    private Map<String, Object> buildMessagePayload(Message message, Long senderId) {
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
        return payload;
    }

    /**
     * 从 Principal 获取用户ID
     */
    private Long getUserIdFromPrincipal(Principal principal) {
        if (principal instanceof WebSocketAuthInterceptor.StompPrincipal) {
            return ((WebSocketAuthInterceptor.StompPrincipal) principal).getUserId();
        }
        throw new RuntimeException("未认证的用户");
    }
}
