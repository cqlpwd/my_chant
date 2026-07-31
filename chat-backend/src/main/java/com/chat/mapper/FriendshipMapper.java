package com.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chat.entity.Friendship;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 好友关系 Mapper
 */
public interface FriendshipMapper extends BaseMapper<Friendship> {

    /**
     * 检查是否为好友关系
     */
    @Select("SELECT COUNT(*) FROM t_friendship WHERE " +
            "(user_id = #{userId1} AND friend_id = #{userId2}) OR " +
            "(user_id = #{userId2} AND friend_id = #{userId1})")
    int isFriend(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * 获取用户的好友列表（含好友信息）
     */
    @Select("SELECT u.id, u.username, u.nickname, u.avatar, u.signature, u.status, u.gender " +
            "FROM t_user u INNER JOIN t_friendship f ON " +
            "(f.user_id = #{userId} AND f.friend_id = u.id) OR " +
            "(f.friend_id = #{userId} AND f.user_id = u.id) " +
            "GROUP BY u.id ORDER BY u.nickname")
    List<com.chat.entity.User> getFriendsWithInfo(@Param("userId") Long userId);

    /**
     * 删除好友关系
     */
    @Delete("DELETE FROM t_friendship WHERE " +
            "(user_id = #{userId} AND friend_id = #{friendId}) OR " +
            "(user_id = #{friendId} AND friend_id = #{userId})")
    int deleteFriendship(@Param("userId") Long userId, @Param("friendId") Long friendId);
}
