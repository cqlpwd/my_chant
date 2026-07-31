package com.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chat.entity.GroupMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface GroupMessageMapper extends BaseMapper<GroupMessage> {

    /** 获取群聊历史消息（最近 200 条） */
    @Select("SELECT gm.*, u.nickname AS sender_nickname, u.avatar AS sender_avatar " +
            "FROM t_group_message gm " +
            "LEFT JOIN t_user u ON gm.sender_id = u.id " +
            "WHERE gm.group_id = #{groupId} " +
            "ORDER BY gm.created_at DESC LIMIT 200")
    List<java.util.Map<String, Object>> getGroupChatHistory(@Param("groupId") Long groupId);
}
