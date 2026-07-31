package com.chat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.dto.MessageDto;
import com.chat.entity.Friendship;
import com.chat.entity.Message;
import com.chat.entity.User;
import com.chat.mapper.FriendshipMapper;
import com.chat.mapper.MessageMapper;
import com.chat.mapper.UserMapper;
import com.chat.service.MessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息服务实现
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final FriendshipMapper friendshipMapper;

    public MessageServiceImpl(MessageMapper messageMapper,
                               UserMapper userMapper,
                               FriendshipMapper friendshipMapper) {
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.friendshipMapper = friendshipMapper;
    }

    @Override
    @Transactional
    public Message sendMessage(Long senderId, Long receiverId, String content, Integer messageType) {
        // 检查是否为好友关系
        int isFriend = friendshipMapper.isFriend(senderId, receiverId);
        if (isFriend == 0) {
            throw new RuntimeException("双方不是好友关系，无法发送消息");
        }

        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setMessageType(messageType != null ? messageType : 0);
        message.setStatus(0);  // 未读
        messageMapper.insert(message);
        return message;
    }

    @Override
    public List<MessageDto> getChatHistory(Long userId1, Long userId2, Long currentUserId) {
        List<Message> messages = messageMapper.getChatHistory(userId1, userId2);
        // 反转列表使其按时间正序
        Collections.reverse(messages);

        // 收集所有相关用户ID
        Set<Long> userIds = new HashSet<>();
        for (Message msg : messages) {
            userIds.add(msg.getSenderId());
            userIds.add(msg.getReceiverId());
        }

        // 批量查询用户信息
        Map<Long, User> userMap = new HashMap<>();
        for (Long uid : userIds) {
            User user = userMapper.selectById(uid);
            if (user != null) {
                userMap.put(uid, user);
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return messages.stream().map(msg -> {
            MessageDto dto = new MessageDto();
            dto.setId(msg.getId());
            dto.setSenderId(msg.getSenderId());
            dto.setReceiverId(msg.getReceiverId());
            dto.setContent(msg.getContent());
            dto.setMessageType(msg.getMessageType());
            dto.setStatus(msg.getStatus());

            User sender = userMap.get(msg.getSenderId());
            if (sender != null) {
                dto.setSenderNickname(sender.getNickname());
                dto.setSenderAvatar(sender.getAvatar());
            }

            if (msg.getCreatedAt() != null) {
                dto.setCreatedAt(msg.getCreatedAt().format(formatter));
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getConversationList(Long userId) {
        List<Message> latestMessages = messageMapper.getConversationList(userId);
        List<Map<String, Object>> conversations = new ArrayList<>();

        for (Message msg : latestMessages) {
            Long friendId = msg.getSenderId().equals(userId)
                    ? msg.getReceiverId() : msg.getSenderId();
            User friend = userMapper.selectById(friendId);

            Map<String, Object> conv = new HashMap<>();
            conv.put("friendId", friendId);
            conv.put("friendNickname", friend != null ? friend.getNickname() : "未知用户");
            conv.put("friendAvatar", friend != null ? friend.getAvatar() : "");
            conv.put("friendStatus", friend != null ? friend.getStatus() : 0);
            conv.put("lastMessage", msg.getContent());
            conv.put("lastMessageType", msg.getMessageType());
            conv.put("lastMessageSenderId", msg.getSenderId());
            conv.put("lastMessageTime", msg.getCreatedAt() != null
                    ? msg.getCreatedAt().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")) : "");

            // 获取未读消息数
            int unread = messageMapper.getUnreadCount(userId, friendId);
            conv.put("unreadCount", unread);

            conversations.add(conv);
        }

        return conversations;
    }

    @Override
    @Transactional
    public int markAsRead(Long userId, Long friendId) {
        return messageMapper.markAsRead(userId, friendId);
    }

    @Override
    public Map<String, Integer> getUnreadCount(Long userId, Long friendId) {
        Map<String, Integer> result = new HashMap<>();
        if (friendId != null) {
            result.put("unread", messageMapper.getUnreadCount(userId, friendId));
        }
        result.put("totalUnread", messageMapper.getTotalUnreadCount(userId));
        return result;
    }
}
