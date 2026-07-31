package com.chat.websocket;

import com.chat.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * WebSocket 认证拦截器：从 STOMP CONNECT 帧中提取 JWT Token 并认证
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtUtil jwtUtil;

    public WebSocketAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 从 STOMP 头中获取 Token（客户端连接时通过 header 传递）
            String token = accessor.getFirstNativeHeader("Authorization");

            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            if (token != null && jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);

                // 设置 WebSocket 会话的认证用户
                Principal principal = new StompPrincipal(userId, username);
                accessor.setUser(principal);

                log.info("WebSocket 用户认证成功: userId={}, username={}", userId, username);
            } else {
                log.warn("WebSocket 认证失败：Token 无效");
                throw new IllegalArgumentException("无效的认证 Token");
            }
        }

        return message;
    }

    /**
     * STOMP 认证主体
     */
    public static class StompPrincipal implements Principal {

        private final Long userId;
        private final String name;

        public StompPrincipal(Long userId, String name) {
            this.userId = userId;
            this.name = name;
        }

        public Long getUserId() {
            return userId;
        }

        @Override
        public String getName() {
            // 返回 userId 字符串，与 convertAndSendToUser 中的用户标识匹配
            return String.valueOf(userId);
        }
    }
}
