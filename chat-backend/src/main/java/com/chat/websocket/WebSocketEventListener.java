package com.chat.websocket;

import com.chat.entity.User;
import com.chat.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 事件监听器：处理用户上线/下线
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    /** 在线用户集合：sessionId -> userId */
    public static final Map<String, Long> ONLINE_USERS = new ConcurrentHashMap<>();

    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(UserService userService,
                                   SimpMessagingTemplate messagingTemplate) {
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();

        if (principal instanceof WebSocketAuthInterceptor.StompPrincipal) {
            WebSocketAuthInterceptor.StompPrincipal stompPrincipal =
                    (WebSocketAuthInterceptor.StompPrincipal) principal;
            Long userId = stompPrincipal.getUserId();
            String sessionId = accessor.getSessionId();

            ONLINE_USERS.put(sessionId, userId);

            // 更新用户状态为在线
            User user = new User();
            user.setId(userId);
            user.setStatus(1);
            userService.updateById(user);

            log.info("用户上线: userId={}, sessionId={}", userId, sessionId);

            // 通知好友上线
            notifyFriendsStatusChange(userId, 1);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Long userId = ONLINE_USERS.remove(sessionId);

        if (userId != null) {
            // 检查用户是否还有其他活跃会话
            boolean hasOtherSession = ONLINE_USERS.containsValue(userId);

            if (!hasOtherSession) {
                // 更新用户状态为离线
                User user = new User();
                user.setId(userId);
                user.setStatus(0);
                userService.updateById(user);

                log.info("用户下线: userId={}, sessionId={}", userId, sessionId);

                // 通知好友下线
                notifyFriendsStatusChange(userId, 0);
            }
        }
    }

    /**
     * 通知好友状态变更
     */
    private void notifyFriendsStatusChange(Long userId, int status) {
        Map<String, Object> payload = Map.of(
                "userId", userId,
                "status", status
        );

        // 获取好友列表并通知
        userService.getFriendIds(userId).forEach(friendId -> {
            messagingTemplate.convertAndSendToUser(
                    friendId.toString(),
                    "/queue/friend-status",
                    payload
            );
        });
    }
}
