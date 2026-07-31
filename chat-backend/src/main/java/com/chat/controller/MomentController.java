package com.chat.controller;

import com.chat.dto.ApiResponse;
import com.chat.dto.CreateMomentRequest;
import com.chat.service.MomentService;
import com.chat.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 朋友圈控制器
 */
@RestController
@RequestMapping("/api/moment")
public class MomentController {

    private final MomentService momentService;
    private final JwtUtil jwtUtil;

    public MomentController(MomentService momentService, JwtUtil jwtUtil) {
        this.momentService = momentService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 发布动态
     */
    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> create(@RequestBody CreateMomentRequest request,
                                                    @RequestHeader("Authorization") String token) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
            var moment = momentService.createMoment(userId, request.getContent(),
                    request.getImages(), request.getLocation());
            return ApiResponse.success("发布成功", Map.of("id", moment.getId()));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取朋友圈列表
     */
    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> list(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        List<Map<String, Object>> moments = momentService.getFriendMoments(userId);
        return ApiResponse.success(moments);
    }

    /**
     * 点赞 / 取消点赞
     */
    @PostMapping("/{momentId}/like")
    public ApiResponse<Map<String, Object>> toggleLike(@PathVariable Long momentId,
                                                        @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        Map<String, Object> result = momentService.toggleLike(momentId, userId);
        return ApiResponse.success(result);
    }

    /**
     * 获取点赞列表
     */
    @GetMapping("/{momentId}/likes")
    public ApiResponse<List<Map<String, Object>>> getLikes(@PathVariable Long momentId) {
        return ApiResponse.success(momentService.getLikes(momentId));
    }

    /**
     * 发表评论
     */
    @PostMapping("/{momentId}/comment")
    public ApiResponse<Map<String, Object>> addComment(@PathVariable Long momentId,
                                                        @RequestBody Map<String, Object> body,
                                                        @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        String content = (String) body.get("content");
        Object replyToObj = body.get("replyTo");
        Long replyTo = replyToObj != null ? ((Number) replyToObj).longValue() : null;
        try {
            Map<String, Object> result = momentService.addComment(momentId, userId, content, replyTo);
            return ApiResponse.success(result);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取评论列表
     */
    @GetMapping("/{momentId}/comments")
    public ApiResponse<List<Map<String, Object>>> getComments(@PathVariable Long momentId) {
        return ApiResponse.success(momentService.getComments(momentId));
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/comment/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long commentId,
                                            @RequestHeader("Authorization") String token) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
            momentService.deleteComment(commentId, userId);
            return ApiResponse.success("删除成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 删除动态
     */
    @DeleteMapping("/{momentId}")
    public ApiResponse<Void> deleteMoment(@PathVariable Long momentId,
                                           @RequestHeader("Authorization") String token) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
            momentService.deleteMoment(momentId, userId);
            return ApiResponse.success("删除成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
