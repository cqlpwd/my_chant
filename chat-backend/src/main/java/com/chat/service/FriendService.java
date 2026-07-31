package com.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chat.dto.FriendRequestDto;
import com.chat.entity.FriendRequest;
import com.chat.entity.User;

import java.util.List;

/**
 * 好友服务接口
 */
public interface FriendService extends IService<FriendRequest> {

    /**
     * 发送好友请求
     */
    FriendRequest sendFriendRequest(Long senderId, Long receiverId, String message);

    /**
     * 处理好友请求（同意/拒绝）
     */
    void handleFriendRequest(Long requestId, Long userId, Integer status);

    /**
     * 获取收到的待处理好友请求列表
     */
    List<FriendRequestDto> getPendingRequests(Long userId);

    /**
     * 获取好友列表
     */
    List<User> getFriendList(Long userId);

    /**
     * 删除好友
     */
    void deleteFriend(Long userId, Long friendId);
}
