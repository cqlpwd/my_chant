package com.chat.controller;

import com.chat.dto.ApiResponse;
import com.chat.dto.FriendRequestDto;
import com.chat.entity.FriendRequest;
import com.chat.entity.User;
import com.chat.service.FriendService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 好友控制器：好友操作、好友请求
 */
@RestController
@RequestMapping("/api/friend")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    /**
     * 发送好友请求
     */
    @PostMapping("/request")
    public ApiResponse<Map<String, Object>> sendFriendRequest(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {
        Long userId = (Long) authentication.getPrincipal();
        Long receiverId = Long.valueOf(body.get("receiverId").toString());
        String message = (String) body.getOrDefault("message", "请求添加您为好友");

        try {
            FriendRequest request = friendService.sendFriendRequest(userId, receiverId, message);
            Map<String, Object> data = new HashMap<>();
            data.put("requestId", request.getId());
            data.put("status", request.getStatus());
            return ApiResponse.success("好友请求已发送", data);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 处理好友请求（同意/拒绝）
     */
    @PutMapping("/request/{requestId}")
    public ApiResponse<Void> handleFriendRequest(
            Authentication authentication,
            @PathVariable Long requestId,
            @RequestBody Map<String, Object> body) {
        Long userId = (Long) authentication.getPrincipal();
        Integer status = Integer.valueOf(body.get("status").toString());  // 1-同意 2-拒绝

        try {
            friendService.handleFriendRequest(requestId, userId, status);
            String msg = status == 1 ? "已同意好友请求" : "已拒绝好友请求";
            return ApiResponse.success(msg, null);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取待处理的好友请求列表
     */
    @GetMapping("/requests")
    public ApiResponse<List<FriendRequestDto>> getPendingRequests(
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<FriendRequestDto> requests = friendService.getPendingRequests(userId);
        return ApiResponse.success(requests);
    }

    /**
     * 获取好友列表
     */
    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> getFriendList(
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<User> friends = friendService.getFriendList(userId);

        List<Map<String, Object>> result = friends.stream().map(f -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", f.getId());
            map.put("username", f.getUsername());
            map.put("nickname", f.getNickname());
            map.put("avatar", f.getAvatar());
            map.put("signature", f.getSignature());
            map.put("status", f.getStatus());
            map.put("gender", f.getGender());
            return map;
        }).collect(Collectors.toList());

        return ApiResponse.success(result);
    }

    /**
     * 删除好友
     */
    @DeleteMapping("/{friendId}")
    public ApiResponse<Void> deleteFriend(
            Authentication authentication,
            @PathVariable Long friendId) {
        Long userId = (Long) authentication.getPrincipal();
        try {
            friendService.deleteFriend(userId, friendId);
            return ApiResponse.success("删除成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
