package com.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chat.entity.FriendRequest;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 好友请求 Mapper
 */
public interface FriendRequestMapper extends BaseMapper<FriendRequest> {

    /**
     * 获取收到的待处理好友请求
     */
    @Select("SELECT * FROM t_friend_request " +
            "WHERE receiver_id = #{userId} AND status = 0 ORDER BY created_at DESC")
    List<FriendRequest> getPendingRequests(@Param("userId") Long userId);

    /**
     * 检查是否已存在待处理的好友请求
     */
    @Select("SELECT * FROM t_friend_request WHERE " +
            "sender_id = #{userId1} AND receiver_id = #{userId2} AND status = 0")
    FriendRequest getPendingRequest(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
