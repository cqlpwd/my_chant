package com.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chat.entity.Message;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 消息 Mapper
 */
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 获取两个用户之间的聊天记录（分页从数据库查询）
     */
    @Select("SELECT * FROM t_message WHERE " +
            "(sender_id = #{userId1} AND receiver_id = #{userId2}) OR " +
            "(sender_id = #{userId2} AND receiver_id = #{userId1}) " +
            "ORDER BY created_at DESC")
    List<Message> getChatHistory(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * 获取用户的所有会话列表（每个好友的最后一条消息）
     */
    @Select("SELECT m.* FROM t_message m " +
            "INNER JOIN ( " +
            "  SELECT " +
            "    CASE WHEN sender_id = #{userId} THEN receiver_id ELSE sender_id END AS friend_id, " +
            "    MAX(created_at) AS max_time " +
            "  FROM t_message " +
            "  WHERE sender_id = #{userId} OR receiver_id = #{userId} " +
            "  GROUP BY friend_id " +
            ") latest ON " +
            "(m.sender_id = #{userId} AND m.receiver_id = latest.friend_id AND m.created_at = latest.max_time) OR " +
            "(m.receiver_id = #{userId} AND m.sender_id = latest.friend_id AND m.created_at = latest.max_time) " +
            "ORDER BY m.created_at DESC")
    List<Message> getConversationList(@Param("userId") Long userId);

    /**
     * 获取未读消息数（指定好友）
     */
    @Select("SELECT COUNT(*) FROM t_message " +
            "WHERE sender_id = #{friendId} AND receiver_id = #{userId} AND status = 0")
    int getUnreadCount(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * 获取总未读消息数
     */
    @Select("SELECT COUNT(*) FROM t_message WHERE receiver_id = #{userId} AND status = 0")
    int getTotalUnreadCount(@Param("userId") Long userId);

    /**
     * 将来自某个好友的消息标记为已读
     */
    @Update("UPDATE t_message SET status = 1 " +
            "WHERE sender_id = #{friendId} AND receiver_id = #{userId} AND status = 0")
    int markAsRead(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * 删除 N 天前的消息（定时清理，减轻数据库压力）
     */
    @Delete("DELETE FROM t_message WHERE created_at < DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int deleteMessagesOlderThan(@Param("days") int days);

    /**
     * 仅获取 N 天内的聊天记录（作为客户端本地存储的补充）
     */
    @Select("SELECT * FROM t_message WHERE " +
            "((sender_id = #{userId1} AND receiver_id = #{userId2}) OR " +
            "(sender_id = #{userId2} AND receiver_id = #{userId1})) " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "ORDER BY created_at DESC")
    List<Message> getRecentChatHistory(@Param("userId1") Long userId1,
                                        @Param("userId2") Long userId2,
                                        @Param("days") int days);
}
