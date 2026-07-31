package com.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chat.entity.Moment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 朋友圈动态 Mapper
 */
public interface MomentMapper extends BaseMapper<Moment> {

    /**
     * 查询好友的动态列表（按时间倒序）
     */
    @Select("SELECT DISTINCT m.* FROM t_moment m " +
            "LEFT JOIN t_friendship f1 ON m.user_id = f1.friend_id AND f1.user_id = #{userId} " +
            "LEFT JOIN t_friendship f2 ON m.user_id = f2.user_id AND f2.friend_id = #{userId} " +
            "WHERE m.user_id = #{userId} OR f1.user_id IS NOT NULL OR f2.friend_id IS NOT NULL " +
            "ORDER BY m.created_at DESC")
    List<Moment> findFriendMoments(@Param("userId") Long userId);
}
