package com.chat.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 注册接口 IP 频率限制拦截器
 * 同一 IP 每分钟最多调用注册接口 3 次
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ConcurrentHashMap<String, Deque<Long>> ipTimestamps = new ConcurrentHashMap<>();

    /** 时间窗口：1 分钟 */
    private static final long WINDOW_MILLIS = 60_000;

    /** 窗口内最大请求数 */
    private static final int MAX_REQUESTS = 3;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String ip = getClientIp(request);
        Deque<Long> timestamps = ipTimestamps
                .computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());

        long now = System.currentTimeMillis();

        // 清理过期的时间戳
        while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MILLIS) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= MAX_REQUESTS) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(429);
            response.getWriter().write("{\"code\":429,\"message\":\"操作太频繁，请1分钟后再试\"}");
            return false;
        }

        timestamps.addLast(now);
        return true;
    }

    /**
     * 获取客户端真实 IP（考虑反向代理）
     */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
