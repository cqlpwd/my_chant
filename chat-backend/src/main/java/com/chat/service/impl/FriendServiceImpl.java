package com.chat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.dto.FriendRequestDto;
import com.chat.entity.FriendRequest;
import com.chat.entity.Friendship;
import com.chat.entity.User;
import com.chat.mapper.FriendRequestMapper;
import com.chat.mapper.FriendshipMapper;
import com.chat.mapper.UserMapper;
import com.chat.service.FriendService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 好友服务实现
 */
@Service
public class FriendServiceImpl extends ServiceImpl<FriendRequestMapper, FriendRequest>
        implements FriendService {

    private final FriendRequestMapper friendRequestMapper;
    private final FriendshipMapper friendshipMapper;
    private final UserMapper userMapper;

    public FriendServiceImpl(FriendRequestMapper friendRequestMapper,
                              FriendshipMapper friendshipMapper,
                              UserMapper userMapper) {
        this.friendRequestMapper = friendRequestMapper;
        this.friendshipMapper = friendshipMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public FriendRequest sendFriendRequest(Long senderId, Long receiverId, String message) {
        if (senderId.equals(receiverId)) {
            throw new RuntimeException("不能添加自己为好友");
        }

        // 检查是否已经是好友
        int isFriend = friendshipMapper.isFriend(senderId, receiverId);
        if (isFriend > 0) {
            throw new RuntimeException("已经是好友了");
        }

        // 检查是否已有待处理的请求
        FriendRequest existRequest = friendRequestMapper
                .getPendingRequest(senderId, receiverId);
        if (existRequest != null) {
            throw new RuntimeException("已发送过好友请求，请等待对方处理");
        }

        // 检查对方是否已发送过请求
        FriendRequest reverseRequest = friendRequestMapper
                .getPendingRequest(receiverId, senderId);
        if (reverseRequest != null) {
            // 对方已经发过请求，直接同意
            handleFriendRequest(reverseRequest.getId(), senderId, 1);
            return reverseRequest;
        }

        FriendRequest request = new FriendRequest();
        request.setSenderId(senderId);
        request.setReceiverId(receiverId);
        request.setMessage(message != null ? message : "请求添加您为好友");
        request.setStatus(0);
        friendRequestMapper.insert(request);
        return request;
    }

    @Override
    @Transactional
    public void handleFriendRequest(Long requestId, Long userId, Integer status) {
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null) {
            throw new RuntimeException("好友请求不存在");
        }

        if (!request.getReceiverId().equals(userId)) {
            throw new RuntimeException("无权处理该好友请求");
        }

        if (request.getStatus() != 0) {
            throw new RuntimeException("该好友请求已处理");
        }

        // 更新请求状态
        request.setStatus(status);
        friendRequestMapper.updateById(request);

        // 如果同意，创建双向好友关系
        if (status == 1) {
            // 检查是否已经是好友（防止并发情况）
            int isFriend = friendshipMapper.isFriend(request.getSenderId(), request.getReceiverId());
            if (isFriend > 0) {
                return;
            }

            // user -> friend
            Friendship friendship1 = new Friendship();
            friendship1.setUserId(request.getSenderId());
            friendship1.setFriendId(request.getReceiverId());
            friendshipMapper.insert(friendship1);

            // friend -> user（双向关系）
            Friendship friendship2 = new Friendship();
            friendship2.setUserId(request.getReceiverId());
            friendship2.setFriendId(request.getSenderId());
            friendshipMapper.insert(friendship2);
        }
    }

    @Override
    public List<FriendRequestDto> getPendingRequests(Long userId) {
        List<FriendRequest> requests = friendRequestMapper.getPendingRequests(userId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<FriendRequestDto> result = new ArrayList<>();

        for (FriendRequest request : requests) {
            User sender = userMapper.selectById(request.getSenderId());

            FriendRequestDto dto = new FriendRequestDto();
            dto.setId(request.getId());
            dto.setSenderId(request.getSenderId());
            dto.setReceiverId(request.getReceiverId());
            dto.setMessage(request.getMessage());
            dto.setStatus(request.getStatus());

            if (sender != null) {
                dto.setSenderUsername(sender.getUsername());
                dto.setSenderNickname(sender.getNickname());
                dto.setSenderAvatar(sender.getAvatar());
            }

            if (request.getCreatedAt() != null) {
                dto.setCreatedAt(request.getCreatedAt().format(formatter));
            }

            result.add(dto);
        }

        return result;
    }

    @Override
    public List<User> getFriendList(Long userId) {
        return friendshipMapper.getFriendsWithInfo(userId);
    }

    @Override
    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        int deleted = friendshipMapper.deleteFriendship(userId, friendId);
        if (deleted == 0) {
            throw new RuntimeException("好友关系不存在");
        }
    }
}
