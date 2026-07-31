package com.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chat.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户 Mapper
 */
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查找用户
     */
    @Select("SELECT * FROM t_user WHERE username = #{username}")
    User findByUsername(@Param("username") String username);

    /**
     * 根据手机号查找用户
     */
    @Select("SELECT * FROM t_user WHERE phone = #{phone}")
    User findByPhone(@Param("phone") String phone);

    /**
     * 搜索用户（用于添加好友）
     */
    @Select("SELECT id, username, nickname, avatar, phone, gender, signature, status FROM t_user " +
            "WHERE (username LIKE CONCAT('%', #{keyword}, '%') OR nickname LIKE CONCAT('%', #{keyword}, '%') OR phone LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND id != #{currentUserId} LIMIT 20")
    List<User> searchUsers(@Param("keyword") String keyword, @Param("currentUserId") Long currentUserId);

    /**
     * 获取用户的好友ID列表
     */
    @Select("SELECT friend_id FROM t_friendship WHERE user_id = #{userId} " +
            "UNION SELECT user_id FROM t_friendship WHERE friend_id = #{userId}")
    List<Long> getFriendIds(@Param("userId") Long userId);
}
