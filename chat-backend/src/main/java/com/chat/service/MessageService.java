package com.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chat.dto.MessageDto;
import com.chat.entity.Message;

import java.util.List;
import java.util.Map;

/**
 * 消息服务接口
 */
public interface MessageService extends IService<Message> {

    /**
     * 发送消息
     */
    Message sendMessage(Long senderId, Long receiverId, String content, Integer messageType);

    /**
     * 获取聊天记录
     */
    List<MessageDto> getChatHistory(Long userId1, Long userId2, Long currentUserId);

    /**
     * 获取会话列表
     */
    List<Map<String, Object>> getConversationList(Long userId);

    /**
     * 标记消息为已读
     */
    int markAsRead(Long userId, Long friendId);

    /**
     * 获取未读消息数
     */
    Map<String, Integer> getUnreadCount(Long userId, Long friendId);
}
